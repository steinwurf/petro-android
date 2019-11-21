#!/usr/bin/env python
# encoding: utf-8

import os
import sys
import json
import shutil
import subprocess

project_name = 'petro-android'


def run_command(args, shell=False):
    print("Running: {}".format(args))
    sys.stdout.flush()
    subprocess.check_call(args, shell=shell)


def get_tool_options(properties):
    options = []
    if 'tool_options' in properties:
        # Make sure that the values are correctly comma separated
        for key, value in properties['tool_options'].items():
            if value is None:
                options += ['--{0}'.format(key)]
            else:
                options += ['--{0}={1}'.format(key, value)]

    return options


def get_compile_sdk_version():
    # We extract compileSdkVersion from build.gradle
    with open('build.gradle') as f:
        for line in f:
            # The version assigments follow this format: "name = value"
            tokens = line.strip().split(' ')
            if tokens[0] == 'compileSdkVersion':
                return tokens[2].strip("'\"")
    return None


def configure(properties):

    # Make sure that the previously built APK and the build folder are deleted
    if properties.get('build_distclean'):
        run_command(['./gradlew', 'clean'])

    # The required sdk versions are extracted from build.gradle
    sdk_version = get_compile_sdk_version()
    if sdk_version is None:
        raise Exception('Unable to find compile sdk version')

    # Install the required Android compileSdkVersion
    command = 'echo y | $ANDROID_HOME/tools/android update sdk --all ' \
              '--filter android-{} --no-ui'.format(sdk_version)
    run_command(command, shell=True)

    command = [sys.executable, 'waf', 'resolve']


def build(properties):
    # Gradle builds the APK
    run_command(['./gradlew', 'assembleDebug', '--debug'])


def run_tests(properties):

    run_command(['./gradlew', 'installDebug'])

    # Gradle builds the APK
    run_command(['./gradlew', 'test', '--info'])

    device_id = properties['tool_options']['device_id']
    # Remove any previously installed versions of the app from the device
    command = 'ANDROID_SERIAL={0} ./gradlew uninstallAll'.format(device_id)
    run_command(command, shell=True)

    # Gradle installs the APK on the target device
    command = 'ANDROID_SERIAL={0} ./gradlew installDebug'.format(device_id)
    run_command(command, shell=True)


def install(properties):
    pass


def main():
    argv = sys.argv

    if len(argv) != 3:
        print("Usage: {} <command> <properties>".format(argv[0]))
        sys.exit(0)

    cmd = argv[1]
    properties = json.loads(argv[2])

    if cmd == 'configure':
        configure(properties)
    elif cmd == 'build':
        build(properties)
    elif cmd == 'run_tests':
        run_tests(properties)
    elif cmd == 'install':
        install(properties)
    else:
        print("Unknown command: {}".format(cmd))


if __name__ == '__main__':
    main()
