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


def get_sdk_package_versions():
    # We extract compileSdkVersion and buildToolsVersion from build.gradle
    versions = [None, None]
    with open('build.gradle') as f:
        for line in f:
            # The version assigments follow this format: "name = value"
            tokens = line.strip().split(' ')
            if tokens[0] == 'compileSdkVersion':
                versions[0] = tokens[2].strip("'\"")
            elif tokens[0] == 'buildToolsVersion':
                versions[1] = tokens[2].strip("'\"")
    return versions


def configure(properties):
    # Configure this project with our waf
    command = [sys.executable, 'waf']

    if properties.get('build_distclean'):
        command += ['distclean']

    # Make sure that the previously built APK and the build folder are deleted
    if properties.get('build_distclean'):
        run_command(['./gradlew', 'clean'])

    command += ['configure', '--git_protocol=git@']

    if 'waf_resolve_path' in properties:
        command += ['--resolve_path=' + properties['waf_resolve_path']]

    if 'dependency_project' in properties:
        command += ['--{0}_checkout={1}'.format(
            properties['dependency_project'],
            properties['dependency_checkout'])]

    command += ["--cxx_mkspec={}".format(properties['cxx_mkspec'])]
    command += get_tool_options(properties)

    run_command(command)

    # The required package versions are extracted from build.gradle
    versions = get_sdk_package_versions()
    if None in versions:
        raise Exception('Insufficient package version info', versions)

    # Install the required Android compileSdkVersion
    command = 'echo y | $ANDROID_HOME/tools/android update sdk --all ' \
              '--filter android-{} --no-ui'.format(versions[0])
    run_command(command, shell=True)

    # Install the Android build-tools version
    command = 'echo y | $ANDROID_HOME/tools/android update sdk --all ' \
              '--filter build-tools-{} --no-ui'.format(versions[1])
    run_command(command, shell=True)


def build(properties):
    command = [sys.executable, 'waf', 'build', '-v']
    run_command(command)

    # Gradle builds the APK (this should be run after the waf build)
    run_command(['./gradlew', 'assembleDebug', '--debug'])


def run_tests(properties):
    command = [sys.executable, 'waf', '-v', '--run_tests']
    command += get_tool_options(properties)
    run_command(command)

    # Gradle builds the APK (this should be run after the waf build)
    run_command(['./gradlew', 'test', '--info'])

    device_id = properties['tool_options']['device_id']
    # Remove any previously installed versions of the app from the device
    command = 'ANDROID_SERIAL={0} ./gradlew uninstallAll'.format(device_id)
    run_command(command, shell=True)

    # Gradle installs the APK on the target device
    command = 'ANDROID_SERIAL={0} ./gradlew installDebug'.format(device_id)
    run_command(command, shell=True)


def install(properties):
    command = [sys.executable, 'waf', '-v', 'install']

    if 'install_path' in properties:
        command += ['--install_path={0}'.format(properties['install_path'])]
    if properties.get('install_relative'):
        command += ['--install_relative']

    run_command(command)


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
