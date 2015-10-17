#!/bin/bash
#
# Copyright (c) 2015 by Delphix. All rights reserved.
#

set -o nounset
set -o errexit

source "${CI_SH_LIB}/common.sh"

log_must /usr/bin/sudo sed -i 's/timeout = 1800/timeout = 10800/' $RUNFILE
log_must /usr/bin/ppriv -s EIP=basic -e \
    /opt/zfs-tests/bin/zfstest -a -c $RUNFILE

exit 0
