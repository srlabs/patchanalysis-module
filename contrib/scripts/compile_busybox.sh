#!/bin/bash

set -e
set -x


HOST_TARGET_OPTS="--host arm-linux-androideabi --enable-targets=arm-linux-androideabi,aarch64-linux-gnu,aarch64_be-linux-gnu"

export CROSS_COMPILE="${CROSS_COMPILE}-"

cd $BASE_DIR/busybox

# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
git reset --hard
git clean -d -f -x


# copy make config
cp $BASE_DIR/scripts/busybox_make.conf $BASE_DIR/busybox/.config

# apply patches
echo "Applying patches:"
echo $BASEDIR/patches/busybox_name.patch
patch -p1 < $BASE_DIR/patches/busybox_name.patch

#build
make -j4 CC=$CC

cp busybox $BASE_DIR/builds/libbusybox.so
