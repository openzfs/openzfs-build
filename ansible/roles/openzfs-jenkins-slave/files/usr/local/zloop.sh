#!/bin/bash

set -x
export BITS=32
export UMEM_DEBUG=default,verbose
export UMEM_LOGGING=transaction,contents
set +x

rm -f core*
rm -f ztest.history
rm -f ztest.out

sparc_32=sparc
sparc_64=sparcv9
i386_32=i86
i386_64=amd64
ARCH=`uname -p`
eval 'BIN=$ROOT/usr/bin/${'"${ARCH}_${BITS}"'}'

set -x
export PATH=$BIN
export LD_LIBRARY_PATH=$ROOT/lib/$BITS:$ROOT/usr/lib/$BITS
set +x
echo

[[ -z "$ZLOOP_RUN_TIME" ]] && ZLOOP_RUN_TIME=3600
start_time=$(/usr/bin/date +%s)

while [[ ! -f core* ]] && [[ $ZLOOP_RUN_TIME -gt $(($(/usr/bin/date +%s) - $start_time)) ]]; do
        zopt="-VVVVV"
        zopt="$zopt -m $((RANDOM % 3))"                 # mirror size
        zopt="$zopt -r $((RANDOM % 3 + 3))"             # raid-z size
        zopt="$zopt -R $((RANDOM % 3 + 1))"             # raid-z parity
        zopt="$zopt -v $((RANDOM % 3))"                 # vdevs
        zopt="$zopt -a $(((RANDOM % 2) * 3 + 9))"       # alignment shift
        zopt="$zopt -T $((RANDOM % 200))"               # total run time
        zopt="$zopt -P $((RANDOM % 20 + 20))"           # time per pass
        cmd="ztest $zopt $@"
        echo "`/bin/date '+%m/%d %T'` $cmd" | /bin/tee -a ztest.history
        /bin/cat ztest.history >> ztest.out
        $BIN/$cmd >> ztest.out 2>&1 || exit 1
done

if [[ -f core* ]]; then
        exit 1
else
        exit 0
fi
