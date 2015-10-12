#!/bin/bash
#
# Copyright (c) 2014, 2015 by Delphix. All rights reserved.
#

set -o nounset
set -o errexit

source "${CI_SH_LIB}/common.sh"
source "${CI_SH_LIB}/os-nightly-lib.sh"

#
# The illumos build cannot be run as the root user. The Jenkins
# infrastructure built around running the build should ensure the build
# is not attempted as root. In case that fails for whatever reason, it's
# best to fail early and with a good error message, than failing later
# with an obscure build error.
#
[ $EUID -ne 0 ] || die "build attempted as root user; this is not supported."

log_must wget --quiet \
  https://download.joyent.com/pub/build/illumos/on-closed-bins.i386.tar.bz2 \
  https://download.joyent.com/pub/build/illumos/on-closed-bins-nd.i386.tar.bz2

log_must tar xjpf on-closed-bins.i386.tar.bz2
log_must tar xjpf on-closed-bins-nd.i386.tar.bz2

log_must cp usr/src/tools/env/illumos.sh .

log_must nightly_env_set_var -f "illumos.sh" "NIGHTLY_OPTIONS"     "-nCDlprt"
log_must nightly_env_set_var -f "illumos.sh" "GATE"                "$JOB_NAME-$BUILD_NUMBER"
log_must nightly_env_set_var -f "illumos.sh" "CODEMGR_WS"          "$PWD"
log_must nightly_env_set_var -f "illumos.sh" "ON_CLOSED_BINS"      "$PWD/closed"
log_must nightly_env_set_var -f "illumos.sh" "ONNV_BUILDNUM"       "151014"
log_must nightly_env_set_var -f "illumos.sh" "ENABLE_IPP_PRINTING" "#"
log_must nightly_env_set_var -f "illumos.sh" "ENABLE_SMB_PRINTING" "#"
log_must nightly_env_set_var -f "illumos.sh" "GCC_ROOT"            "/opt/gcc-4.4.4"
log_must nightly_env_set_var -f "illumos.sh" "CW_GCC_DIR"          "\${GCC_ROOT}/bin"
log_must nightly_env_set_var -f "illumos.sh" "__GNUC"              ""
log_must nightly_env_set_var -f "illumos.sh" "CW_NO_SHADOW"        "1"
log_must nightly_env_set_var -f "illumos.sh" "ONLY_LINT_DEFS"      "-I/opt/SUNWspro/sunstudio12.1/prod/include/lint"
log_must nightly_env_set_var -f "illumos.sh" "PERL_VERSION"        "5.16.1"
log_must nightly_env_set_var -f "illumos.sh" "PERL_ARCH"           "i86pc-solaris-thread-multi-64int"
log_must nightly_env_set_var -f "illumos.sh" "PERL_PKGVERS"        "-5161"

log_must cp usr/src/tools/scripts/nightly.sh .
log_must chmod +x nightly.sh
log_must nightly_run ./nightly.sh "$PWD" "illumos.sh"

log_must mail_msg_is_clean "$PWD" "Build errors" "Build warnings"
log_must mail_msg_is_clean "$PWD" "Build warnings" "Elapsed build time"
log_must mail_msg_is_clean "$PWD" "Build errors (non-DEBUG)" "Build warnings (non-DEBUG)"
log_must mail_msg_is_clean "$PWD" "Build warnings (non-DEBUG)" "Elapsed build time (non-DEBUG)"
log_must mail_msg_is_clean "$PWD" "Build errors (DEBUG)" "Build warnings (DEBUG)"
log_must mail_msg_is_clean "$PWD" "Build warnings (DEBUG)" "Elapsed build time (DEBUG)"
log_must mail_msg_is_clean "$PWD" "lint warnings src" "lint noise differences src"
log_must mail_msg_is_clean "$PWD" "cstyle/hdrchk errors" "Find core files"
log_must mail_msg_is_clean "$PWD" "Validating manifests against proto area" "Check ELF runtime attributes"

log_must ln -sf usr/src/tools/scripts/bldenv.sh .
log env -i ksh93 bldenv.sh -d "illumos.sh" -c "git nits -b 'HEAD^'" 2>&1

exit 0
