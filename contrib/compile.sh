#!/bin/bash -e


# This script helps building the needed dependency binaries, and saves all build logs

# busybox
#	commit: cccf8e735da9eb62f1de021534ca50255d82e931
# binutils_gdb used version: 2.27
#	commit: 2870b1ba83fc0e0ee7eadf72d614a7ec4591b169

usage()
{
    echo >&2 "Usage: $0 -t {android|android64|host} [-f] [-h]"
    echo >&2 "   -t <target>   Target to build for"
    exit 1
}

while getopts hfgut: o
do
    case "$o" in
        t)      export target="${OPTARG}";;
    esac
done
shift $(($OPTIND-1))

case ${target} in
        android|android64) ;;
        *)             usage;
esac;

export BASE_DIR="$( cd "$( dirname $0 )" && pwd )"
export OUTPUT_DIR=$BASE_DIR/builds

# set platform
MACH=$(uname -m)
KERN=$(uname -s)

case ${KERN} in
        Darwin) HOST="darwin-${MACH}";;
        Linux)  HOST="linux-${MACH}";;
        *)      echo "Unknown platform ${KERN}-${MACH}!"; exit 1;;
esac

case ${target} in
	android)
		export SYSROOT="${NDK_DIR}/platforms/android-28/arch-arm/"
		export MSD_CONFIGURE_OPTS="--host arm-linux-androideabi --prefix=${MSD_DESTDIR}"
		export PATH=${PATH}:${NDK_DIR}/toolchains/arm-linux-androideabi-4.9/prebuilt/${HOST}/bin/
		# Make sure that "clang" points to NDK clang, not to /usr/bin/clang of the host system
		export PATH=${NDK_DIR}/toolchains/llvm/prebuilt/${HOST}/bin/:${PATH}
		export CROSS_COMPILE=arm-linux-androideabi
		export RANLIB=arm-linux-androideabi-ranlib
		export CC=armv7a-linux-androideabi28-clang
		export CFLAGS="--sysroot=${SYSROOT} -nostdlib -I${NDK_DIR}/sysroot/usr/include/  -I${NDK_DIR}/sysroot/usr/include/arm-linux-androideabi/ -DANDROID_ABI=armeabi-v7a"
		export CPPFLAGS="-I${NDK_DIR}/sysroot/usr/include/  -I${NDK_DIR}/sysroot/usr/include/arm-linux-androideabi/"
		export LDFLAGS="--sysroot=${SYSROOT} -Wl,-rpath-link=${NDK_DIR}/toolchains/llvm/prebuilt/${HOST}/sysroot/usr/lib/arm-linux-androideabi/,-L${NDK_DIR}/toolchains/llvm/prebuilt/${HOST}/sysroot/usr/lib/arm-linux-androideabi/"
		# libtool filters out -lc from command line since it thinks it isn't required.
		# However, it actually is required. Passing -Wl,-lc instead will be equivalent
		# to -lc and not gets filtered out.
		export LIBS="-Wl,-lc -lm -lgcc"
		;;
	android64)
		export SYSROOT="${NDK_DIR}/platforms/android-28/arch-arm64/"
		export MSD_CONFIGURE_OPTS="--host aarch64-linux-android --prefix=${MSD_DESTDIR}"
		export PATH=${PATH}:${NDK_DIR}/toolchains/arm-linux-androideabi-4.9/prebuilt/${HOST}/bin/
		# Make sure that "clang" points to NDK clang, not to /usr/bin/clang of the host system
		export PATH=${NDK_DIR}/toolchains/llvm/prebuilt/${HOST}/bin/:${PATH}
		export CROSS_COMPILE=aarch64-linux-android
		export RANLIB=aarch64-linux-android-ranlib
		export CC=aarch64-linux-android28-clang
		export CFLAGS="--sysroot=${SYSROOT} -nostdlib"
		export CPPFLAGS="-I${NDK_DIR}/sysroot/usr/include/  -I${NDK_DIR}/sysroot/usr/include/aarch64-linux-android/"
		export LDFLAGS="--sysroot=${SYSROOT} -Wl,-rpath-link=${SYSROOT}/usr/lib/arm-linux-androideabi/,-L${SYSROOT}/usr/lib/arm-linux-androideabi/,-L${NDK_DIR}/toolchains/llvm/prebuilt/${HOST}/sysroot/usr/lib/arm-linux-androideabi/"
		# libtool filters out -lc from command line since it thinks it isn't required.
		# However, it actually is required. Passing -Wl,-lc instead will be equivalent
		# to -lc and not gets filtered out.
		export LIBS="-Wl,-lc -lm -lgcc"
		;;
esac

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
