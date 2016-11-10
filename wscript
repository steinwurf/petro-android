#! /usr/bin/env python
# encoding: utf-8

import os
import shutil


from waflib import Task, Errors, Logs
from waflib.TaskGen import feature, after_method

import waflib.extras.wurf_options

APPNAME = 'petro_android'
VERSION = '0.0.0'

# /------------------------------------------------------------
# /----------------- ANDROID SPECIFIC -------------------------
# /------------------------------------------------------------


@feature('cxx')
@after_method('apply_link')
def copy_lib_to_libs_folder(self):
    """
    When building an Android application from the IDE native libraries placed
    in the libs folder will be packaged in the apk. So we copy the shared libs
    we build there.
    """
    if not self.bld.is_mkspec_platform('android'):
        return

    for x in self.features:
        if x == 'cxxshlib':
            break
    else:
        return

    input_libs = self.link_task.outputs
    output_libs = []
    for i in input_libs:
        outlib = os.path.basename(i.abspath())

        outlib = self.bld.root.make_node(
            os.path.abspath(os.path.join(
                self.bld.srcnode.abspath(),
                'app',
                'src',
                'main',
                'jniLibs',
                'armeabi',
                outlib)))

        output_libs.append(outlib)

    cpy_tsk = self.create_task('AndroidCopyFileTask')
    cpy_tsk.set_inputs(input_libs)
    cpy_tsk.set_outputs(output_libs)
    cpy_tsk.chmod = self.link_task.chmod


class AndroidCopyFileTask(Task.Task):

    """Performs the copying of generated files to the Android project."""

    color = 'PINK'

    def run(self):

        for src_node, tgt_node in zip(self.inputs, self.outputs):
            src = src_node.abspath()
            tgt = tgt_node.abspath()

            # Following is for shared libs and stale inodes (-_-)
            try:
                os.remove(tgt)
            except OSError:
                pass

            # Make sure the output directories are available
            try:
                os.makedirs(os.path.dirname(tgt))
            except OSError:
                pass

            # Copy the file
            try:
                shutil.copy2(src, tgt)
                os.chmod(tgt, self.chmod)
            except IOError as e:
                Logs.error("The copy file step failed: {0}".format(e))
                try:
                    os.stat(src)
                except (OSError, IOError):
                    Logs.error('File %r does not exist' % src)
                raise Errors.WafError('Could not install the file %r' % tgt)

# ------------------------------------------------------------/
# ----------------- ANDROID SPECIFIC -------------------------/
# ------------------------------------------------------------/


def options(opt):

    opt.load('wurf_common_tools')


def resolve(ctx):

    import waflib.extras.wurf_dependency_resolve as resolve

    ctx.load('wurf_common_tools')

    ctx.add_dependency(resolve.ResolveVersion(
        name='waf-tools',
        git_repository='github.com/steinwurf/waf-tools.git',
        major=3))

    ctx.add_dependency(resolve.ResolveVersion(
        name='petro',
        git_repository='github.com/steinwurf/petro.git',
        major=6))


def configure(conf):

    conf.load("wurf_common_tools")
    conf.check_cxx(lib='android')


def build(bld):

    bld.load("wurf_common_tools")

    bld.recurse('jni')

    bld.env.append_unique(
        'DEFINES_STEINWURF_VERSION',
        'STEINWURF_PETRO_ANDROID_VERSION="{}"'.format(VERSION))

    if bld.is_toplevel():

        # Install the APK files from "app/build/outputs/apk"
        if bld.has_tool_option('install_path'):
            install_path = bld.get_tool_option('install_path')
            install_path = os.path.abspath(os.path.expanduser(install_path))
            start_dir = bld.path.find_dir('app/build/outputs/apk')
            relative_dir = bld.bldnode.path_from(bld.srcnode)
            bld.install_files(os.path.join(install_path, relative_dir),
                              start_dir.ant_glob('*.apk'),
                              cwd=start_dir)

