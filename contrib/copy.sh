#!/bin/bash

export BASE_DIR="$( cd "$( dirname $0 )" && pwd )"

JNI_32_DIR=$BASE_DIR/../patchanalysis_module/src/main/jniLibs/armeabi-v7a/
JNI_64_DIR=$BASE_DIR/../patchanalysis_module/src/main/jniLibs/arm64-v8a/


cp $BASE_DIR/builds/32/*.so $JNI_32_DIR
cp $BASE_DIR/builds/64/*.so $JNI_64_DIR
