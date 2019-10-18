#!/bin/bash

set -e
set -x

cd ${BASE_DIR}/sigtool
${NDK_DIR}/ndk-build
