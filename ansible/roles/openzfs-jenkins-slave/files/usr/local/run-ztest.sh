#!/bin/bash
#
# Copyright (c) 2015 by Delphix. All rights reserved.
#

set -o nounset

source "${CI_SH_LIB}/common.sh"

ZFS_DEBUG=""
[[ "$ENABLE_WATCHPOINTS" = "yes" ]] && ZFS_DEBUG="watch"

ZFS_DEBUG=$ZFS_DEBUG ${CI_SH_LIB}/zloop.sh -t $RUN_TIME -c . -f .
result=$?

if [[ $result -ne 0 ]]; then
	if [[ -r ztest.cores ]]; then
	    log_must cat ztest.cores
	fi

	if [[ -r core ]]; then
	    log_must echo '::status' | log_must mdb core
	    log_must echo '::stack' | log_must mdb core
	fi
fi

log_must tail -n 30 ztest.out

exit $result
