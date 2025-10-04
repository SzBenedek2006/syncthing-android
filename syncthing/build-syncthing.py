from __future__ import print_function

import os
import os.path
import platform
import subprocess
import sys

PLATFORM_DIRS = {
    'Windows': 'windows-x86_64',
    'Linux': 'linux-x86_64',
    'Darwin': 'darwin-x86_64',
}

# The values here must correspond with those in ../docker/prebuild.sh
BUILD_TARGETS = [
    {'arch':'arm','goarch':'arm','jni_dir':'armeabi','cc':'armv7a-linux-androideabi{}-clang'},
    {'arch':'arm64','goarch':'arm64','jni_dir':'arm64-v8a','cc':'aarch64-linux-android{}-clang'},
    {'arch':'x86','goarch':'386','jni_dir':'x86','cc':'i686-linux-android{}-clang'},
    {'arch':'x86_64','goarch':'amd64','jni_dir':'x86_64','cc':'x86_64-linux-android{}-clang'},
]

def fail(message, *args, **kwargs):
    print((message % args).format(**kwargs))
    sys.exit(1)

def get_min_sdk(project_dir):
    with open(os.path.join(project_dir, 'app', 'build.gradle.kts')) as file_handle:
        for line in file_handle:
            tokens = list(filter(None, line.split()))
            if len(tokens) == 3 and tokens[0] == 'minSdk':
                return int(tokens[2])
    fail('Failed to find minSdkVersion')

def get_ndk_home():
    """
    Locate the Android NDK directory. Try in order:
      1. ANDROID_NDK_HOME env var
      2. sdk.dir + ndk.dir entries in local.properties
      3. NDK_VERSION + ANDROID_HOME env var
    """

    """
    # 1) check explicit env var
    ndk_home = os.environ.get('ANDROID_NDK_HOME')
    if ndk_home:
        return ndk_home

    # 2) try reading local.properties
    project_root = os.path.realpath(os.path.join(os.path.dirname(__file__), '..'))
    local_props_path = os.path.join(project_root, 'local.properties')
    if os.path.isfile(local_props_path):
        with open(local_props_path) as lp:
            for line in lp:
                if line.startswith('ndk.dir'):
                    _, val = line.split('=', 1)
                    return val.strip()

    """
    # 3) fallback to NDK_VERSION + ANDROID_HOME
    ndk_ver = os.environ.get('NDK_VERSION')
    android_home = os.environ.get('ANDROID_HOME')
    if ndk_ver and android_home:
        return os.path.join(android_home, 'ndk', ndk_ver)

    # nothing found
    print(
        f"ANDROID_HOME: {os.environ.get('ANDROID_HOME', '')}\n",
        f"ANDROID_NDK_HOME: {os.environ.get('ANDROID_NDK_HOME', '')}\n",
        f"NDK_VERSION: {os.environ.get('NDK_VERSION', '')}"
    )
    fail('ANDROID_NDK_HOME or NDK_VERSION and ANDROID_HOME environment variable must be defined')

if platform.system() not in PLATFORM_DIRS:
    fail('Unsupported python platform %s. Supported platforms: %s', platform.system(), ', '.join(PLATFORM_DIRS.keys()))

module_dir = os.path.dirname(os.path.realpath(__file__))
project_dir = os.path.realpath(os.path.join(module_dir, '..'))
build_dir = os.path.join(module_dir, 'gobuild')
go_build_dir = os.path.join(build_dir, 'go-packages')
syncthing_dir = os.path.join(module_dir, 'src/github.com/syncthing/syncthing')
min_sdk = get_min_sdk(project_dir)

# Fetch tags
subprocess.check_call(['git', '-C', syncthing_dir, 'fetch', '--tags'])

for target in BUILD_TARGETS:
    print('Building syncthing for', target['arch'])
    environ = os.environ.copy()
    environ.update({'GO111MODULE':'on', 'CGO_ENABLED':'1'})
    try:
        subprocess.check_call(['go','version'], env=environ, cwd=syncthing_dir)
    except subprocess.CalledProcessError as e:
        #print(f"Exception:\n{e}")
        fail("ERROR: Something went wrong with go!")
    except FileNotFoundError as e:
        fail("\n==============================\n\tGO NOT FOUND!\n==============================\n")

    subprocess.check_call(['go','run','build.go','version'], env=environ, cwd=syncthing_dir)


    cc = os.path.join(
        get_ndk_home(), 'toolchains', 'llvm', 'prebuilt', PLATFORM_DIRS[platform.system()], 'bin', target['cc'].format(min_sdk)
    )



    subprocess.check_call([
        'go',
        'run',
        'build.go',
        '-goos',
        'android',
        '-goarch', target['goarch'],
        '-cc', cc,
        '-pkgdir',os.path.join(go_build_dir,target['goarch']),
        '-no-upgrade',
        'build',
    ], env=environ, cwd=syncthing_dir)

    target_dir = os.path.join(project_dir,'app/src/main/jniLibs',target['jni_dir'])
    os.makedirs(target_dir, exist_ok=True)
    artifact = os.path.join(target_dir,'libsyncthing.so')
    if os.path.exists(artifact): os.unlink(artifact)
    os.rename(os.path.join(syncthing_dir,'syncthing'), artifact)
    print('Finished build for', target['arch'])

print('All builds finished')
