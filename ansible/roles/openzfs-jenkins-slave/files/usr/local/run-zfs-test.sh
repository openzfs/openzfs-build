#!/bin/bash
#
# Copyright (c) 2015 by Delphix. All rights reserved.
#

set -o nounset

source "${CI_SH_LIB}/common.sh"

log_must /usr/bin/sudo sed -i 's/timeout = .*/timeout = 10800/' $RUNFILE
log_must /usr/bin/ppriv -s EIP=basic -e \
    /opt/zfs-tests/bin/zfstest -a -c $RUNFILE 2>&1 | /usr/bin/tee results.txt

log_must /usr/bin/python "${CI_SH_LIB}/zfstest-report.py" results.txt

exit 0
