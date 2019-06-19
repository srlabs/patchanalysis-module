#!/bin/sh

cd $BASE_DIR/binutils-gdb/bfd
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4
cd ../libiberty
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4
cd ../opcodes
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4
cd ../binutils
./configure $HOST_TARGET_OPTS --disable-option-checking  CXX=${CROSS_COMPILE}-g++ CC=${CROSS_COMPILE}-gcc  --disable-nls
make -j4

pwd
cp objdump $BASE_DIR/builds/libobjdump.so

#perl -i.bak -pe '$_="// REMOVED\n if /^#include <stdio_ext.h>/' libiberty/fopen_unlocked.c
#perl -i.bak -pe '$_="// REMOVED\n if /^#include <sys\/sysctl.h.h>/' libiberty/physmem.c

#perl -i.bak -pe '$_="// REMOVED\n" if /^\s*#\s*include\s*<stdio_ext.h>/' intl/localealias.c


# export CFLAGS="$CFLAGS -DHAVE_STDIO_EXT_H=0"
