#!/bin/sh


# copy make config
cp $BASE_DIR/scripts/busybox_make.conf ../busybox/.config

cd $BASE_DIR/busybox

# apply patches
echo "Applying patches:"
echo $BASEDIR/patches/busybox_name.patch
git reset --hard
git clean -f
patch -p1 < $BASE_DIR/patches/busybox_name.patch

#build
make -j4

cp busybox $BASE_DIR/builds/libbusybox.so
