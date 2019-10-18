#!/bin/bash

set -e
set -x

echo "target: ${target}"
case ${target} in
	android)
		HOST_TARGET_OPTS="--host arm-linux-androideabi --enable-targets=arm-linux-androideabi,aarch64-linux-gnu,aarch64_be-linux-gnu"
		;;
	android64)
	    HOST_TARGET_OPTS="--host aarch64-linux-androideabi --enable-targets=arm-linux-androideabi,aarch64-linux-gnu,aarch64_be-linux-gnu"
	    ;;
esac


cd $BASE_DIR/binutils-gdb/
# Make sure all artefacts from previous compilation runs are deleted
# Required when switching between different architectures (host, arm, aarch64)
git reset --hard
git clean -d -f -x

cd bfd/
./configure $HOST_TARGET_OPTS --disable-option-checking --disable-nls
make -j4
cd ../libiberty
./configure $HOST_TARGET_OPTS --disable-option-checking --disable-nls
make -j4
cd ../opcodes
./configure $HOST_TARGET_OPTS --disable-option-checking --disable-nls
make -j4
cd ../binutils
./configure $HOST_TARGET_OPTS --disable-option-checking --disable-nls
make -j4 objdump
