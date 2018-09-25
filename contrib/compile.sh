#!/bin/bash -e


# This script helps building the needed dependency binaries, and saves all build logs

# busybox
#	commit: cccf8e735da9eb62f1de021534ca50255d82e931
# binutils_gdb used version: 2.27
#	commit: 2870b1ba83fc0e0ee7eadf72d614a7ec4591b169


export BASE_DIR="$( cd "$( dirname $0 )" && pwd )"
export OUTPUT_DIR=$BASE_DIR/builds

# init git submodules (if necessary)
git submodule init busybox
git submodule init binutils-gdb

# update submodules
git submodule update busybox
git submodule update binutils-gdb


# make sure that NDK_DIR is set
if env | grep -q ^NDK_DIR
then
	echo "OK: NDK_DIR environment variable is set."
else
	echo "Error: Please make sure you installed Android NDK and export NDK_DIR to the correct path!"
	exit 1
fi

# building binaries
TARGETS="sigtool busybox binutils-gdb"
cd $OUTPUT_DIR
for i in ${TARGETS}; do
    echo -n "Building $i..."
    if ${BASE_DIR}/scripts/compile_$i.sh > $i.compile_log 2>&1;then
        echo OK
    else
        echo "Failed!"
        echo "Please view log file $OUTPUT_DIR/$i.compile_log"
        exit 1
    fi
done



