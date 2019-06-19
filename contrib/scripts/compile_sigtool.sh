#!/bin/bash

set -e
set -x

cd ${BASE_DIR}/sigtool
${NDK_DIR}/ndk-build

cp ${BASE_DIR}/sigtool/libs/armeabi-v7a/libsigtool.so ${BASE_DIR}/builds/
