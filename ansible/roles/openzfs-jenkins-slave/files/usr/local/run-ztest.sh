#!/bin/bash
#
# Copyright (c) 2015 by Delphix. All rights reserved.
#

set -o nounset

source "${CI_SH_LIB}/common.sh"

log ${CI_SH_LIB}/zloop.sh -f .
result=$?

log cat ztest.out | tail -n 30

if [[ 0 -ne $result ]]; then
        log_must echo "::stack" | mdb core
fi

exit $result
