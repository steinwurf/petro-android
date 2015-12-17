package com.steinwurf.petro;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class BothExtractorDecoder extends Thread {

    private static final String TAG = "BothExtractorDecoder";
    private static final int TIMEOUT_US = 10000;

    public class MyAudioTrack extends AudioTrack
    {

        public MyAudioTrack(int streamMusic, int sampleRate, int channelOutStereo, int encodingPcm16bit, int minBufferSize, int modeStream) {
            super(streamMusic, sampleRate, channelOutStereo, encodingPcm16bit, minBufferSize, modeStream);
        }

        public int getFrameCount()
        {
            return getNativeFrameCount();
        }
    }

    public class IndexedBufferInfo
    {
        IndexedBufferInfo(int index, BufferInfo bufferInfo)
        {
            this.index = index;
            this.bufferInfo = bufferInfo;
        }

        BufferInfo bufferInfo;
        int index;
    }

    public class CodecState
    {
        MediaCodec mCodec;

        ByteBuffer mCSD0;
        ByteBuffer mCSD1;
        ByteBuffer mCSD2;

        ByteBuffer mInputBuffers[];
        ByteBuffer mOutputBuffers[];
        List<Integer> mAvailableInputBufferIndices;

        List<IndexedBufferInfo> mAvailableOutputBufferInfos;
        MyAudioTrack mAudioTrack;
        int mNumFramesWritten;

        CodecState()
        {
            mAvailableInputBufferIndices = new ArrayList<>();
            mAvailableOutputBufferInfos = new ArrayList<>();
            MediaSync sync = new MediaSync();
        }

    }
    private static final String AUDIO = "audio/";
    private static final String VIDEO = "video/";
    private MediaExtractor mExtractor;
    private long mStartTimeRealUs;

    private Hashtable<Integer, CodecState> mStateByTrackIndex;

    private boolean mEosReceived;

    public BothExtractorDecoder()
    {
        mStateByTrackIndex = new Hashtable<>();
        mEosReceived = false;
        mStartTimeRealUs = -1l;
    }

    public boolean init(Surface surface, String filePath) {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        boolean haveAudio = false;
        boolean haveVideo = false;
        for (int i = 0; i < mExtractor.getTrackCount(); ++i) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Surface s = null;
            if (!haveAudio && mime.startsWith(AUDIO)) {
                haveAudio = true;
            } else if (!haveVideo && mime.startsWith(VIDEO)) {
                haveVideo = true;
                s = surface;
            } else {
                continue;
            }
            mExtractor.selectTrack(i);

            CodecState state = new CodecState();
            mStateByTrackIndex.put(i, state);

            state.mNumFramesWritten = 0;
            try {
                state.mCodec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            state.mCodec.configure(
                    format,
                    s,
                    null /* crypto */,
                    0 /* flags */);

            int j = 0;
            state.mCSD0 = format.getByteBuffer("csd-0");
            state.mCSD1 = format.getByteBuffer("csd-1");
            state.mCSD2 = format.getByteBuffer("csd-2");
        }
        assert haveAudio;
        assert haveVideo;

        for (int i = 0; i < mStateByTrackIndex.size(); ++i) {
            CodecState state = mStateByTrackIndex.get(i);

            state.mCodec.start();

            state.mInputBuffers = state.mCodec.getInputBuffers();
            state.mOutputBuffers = state.mCodec.getOutputBuffers();

            for (ByteBuffer srcBuffer : new ByteBuffer[]{state.mCSD0, state.mCSD1, state.mCSD1}) {
                if (srcBuffer == null)
                    break;
                int index = state.mCodec.dequeueInputBuffer(-1l);
                ByteBuffer dstBuffer = state.mInputBuffers[index];

                dstBuffer.clear();
                dstBuffer.put(srcBuffer);
                dstBuffer.clear();
                state.mCodec.queueInputBuffer(
                        index, 0, dstBuffer.capacity(), 0l, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
            }
        }
        return true;
    }

    private void onOutputFormatChanged(CodecState state)
    {
        Log.d(TAG, "onOutputFormatChanged");
        MediaFormat format = state.mCodec.getOutputFormat();

        String mime = format.getString(MediaFormat.KEY_MIME);

        if (mime.startsWith(AUDIO)) {
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            assert channelCount == 2;
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            state.mAudioTrack = new MyAudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT),
                    AudioTrack.MODE_STREAM);

            state.mNumFramesWritten = 0;
        }
    }

    private void doMoreStuff()
    {
        Log.d(TAG, "doing more stuff...");
        for (int i = 0; i < mStateByTrackIndex.size(); ++i) {
            Log.d(TAG, "mStateByTrackIndex");
            CodecState state = mStateByTrackIndex.get(i);
            while(true) {
                Log.d(TAG, "dequeueInputBuffer");
                int index = state.mCodec.dequeueInputBuffer(TIMEOUT_US);

                if (index >= 0) {
                    Log.d(TAG, "dequeued input buffer on track");
                            state.mAvailableInputBufferIndices.add(index);
                } else {
                    break;
                }
            }

            while (true) {
                BufferInfo info = new BufferInfo();
                int outIndex = state.mCodec.dequeueOutputBuffer(info, TIMEOUT_US);
                if (outIndex >= 0)
                {
                    Log.d(TAG, "dequeued output buffer on track");
                    state.mAvailableOutputBufferInfos.add(new IndexedBufferInfo(outIndex, info));
                }
                else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                    onOutputFormatChanged(state);
                } else if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    state.mOutputBuffers = state.mCodec.getOutputBuffers();
                } else {
                    Log.d(TAG, "break");
                    break;
                }
            }
        }

        while (true) {
            Log.d(TAG, "getSampleTrackIndex");
            int trackIndex = mExtractor.getSampleTrackIndex();

            if (trackIndex >= 0) {
                Log.d(TAG, "trackIndex >= 0");
                break;
            } else {
                Log.d(TAG, "else");
                CodecState state = mStateByTrackIndex.get(trackIndex);

                if (state.mAvailableInputBufferIndices.isEmpty()) {
                    break;
                }

                int index = state.mAvailableInputBufferIndices.remove(0);

                ByteBuffer dstBuffer = state.mInputBuffers[index];

                int sampleSize = mExtractor.readSampleData(dstBuffer, 0);

                long timeUs = mExtractor.getSampleTime();
                state.mCodec.queueInputBuffer(index, 0, sampleSize, timeUs, 0);

                mExtractor.advance();
            }
        }

        long nowUs = System.currentTimeMillis();

        if (mStartTimeRealUs < 0l) {
            mStartTimeRealUs = nowUs + 1000000l;
        }

        for (int i = 0; i < mStateByTrackIndex.size(); ++i) {
            CodecState state = mStateByTrackIndex.get(i);

            while (!state.mAvailableOutputBufferInfos.isEmpty()) {
                Log.d(TAG, "!state.mAvailableOutputBufferInfos.isEmpty()");
                IndexedBufferInfo info = state.mAvailableOutputBufferInfos.get(0);
                long whenRealUs = info.bufferInfo.presentationTimeUs + mStartTimeRealUs;
                long lateByUs = nowUs - whenRealUs;

                if (lateByUs > -10000l) {
                    boolean release = true;

                    if (lateByUs > 30000l) {
                        //track %d buffer late by %lld us, dropping.
                        state.mCodec.releaseOutputBuffer(info.index, false);
                    } else {
                        if (state.mAudioTrack != null) {
                            ByteBuffer srcBuffer =
                                    state.mOutputBuffers[info.index];

                            renderAudio(state, info.bufferInfo, srcBuffer);

                            if (info.bufferInfo.size > 0) {
                                release = false;
                            }
                        }

                        if (release) {
                            state.mCodec.releaseOutputBuffer(info.index, true);
                        }
                    }

                    if (release) {
                        state.mAvailableOutputBufferInfos.remove(0);
                    } else {
                        break;
                    }
                } else {
                    //track %d buffer early by %lld us.
                    break;
                }
            }
        }
        Log.d(TAG, "did more stuff...");
    }

    public static int getBytesPerSample(int audioFormat)
    {
        switch (audioFormat) {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_DEFAULT:
                return 2;
            case AudioFormat.ENCODING_PCM_FLOAT:
                return 4;
            case AudioFormat.ENCODING_INVALID:
            default:
                throw new IllegalArgumentException("Bad audio format " + audioFormat);
        }
    }

    private void renderAudio(CodecState state, BufferInfo info, ByteBuffer buffer) {

        if (state.mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
            state.mAudioTrack.play();
        }

        int numFramesPlayed = state.mAudioTrack.getPlaybackHeadPosition();
        int numFramesAvailableToWrite = state.mAudioTrack.getFrameCount() -
                (state.mNumFramesWritten - numFramesPlayed);

        int frameSizeInBytes = state.mAudioTrack.getChannelCount() *
                getBytesPerSample(state.mAudioTrack.getAudioFormat());

        int numBytesAvailableToWrite =
                numFramesAvailableToWrite * frameSizeInBytes;

        int copy = info.size;
        if (copy > numBytesAvailableToWrite) {
            copy = numBytesAvailableToWrite;
        }

        if (copy == 0) {
            return;
        }

        long startTimeUs = System.currentTimeMillis();

        final byte[] chunk = new byte[info.size];
        buffer.get(chunk); // Read the buffer all at once
        buffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN


        int nbytes = state.mAudioTrack.write(chunk, info.offset, info.offset + copy);

        long delayUs = System.currentTimeMillis() - startTimeUs;

        int numFramesWritten = nbytes / frameSizeInBytes;

        if (delayUs > 2000l) {
            //"AudioTrack::write took %lld us, numFramesAvailableToWrite=%u, "numFramesWritten=%u",
        }

        info.offset += nbytes;
        info.size -= nbytes;

        state.mNumFramesWritten += numFramesWritten;
    }

    @Override
    public void run() {
        while(!mEosReceived)
            doMoreStuff();
    }

    public void close() {
        mEosReceived = true;
    }
}
