#!/bin/bash

export BASE_DIR="$( cd "$( dirname $0 )" && pwd )"
cp $BASE_DIR/builds/*.so $BASE_DIR/../patchanalysis_module/src/main/jniLibs/armeabi/
