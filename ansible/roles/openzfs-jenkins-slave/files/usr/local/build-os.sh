#!/bin/bash
#
# Copyright (c) 2014, 2015 by Delphix. All rights reserved.
#

set -o nounset
set -o errexit

source "${CI_SH_LIB}/common.sh"
source "${CI_SH_LIB}/os-nightly-lib.sh"

#
# Updates the nightly environment file. If there's a default value
# provided for the variable we're attempting to set, this value is
# overridden in-place. We need to modify the value of the variable,
# without changing it's location in the file so we don't invalidate any
# later references of the variable. If there isn't an existing default
# value, then the export declaration for the variable is simply appended
# to the end of environment file using the provided value.
#
function nightly_env_set_var
{
	#
	# The environment file is hard-coded. Since we don't use
	# anything other than this value, using a hard-coded value here
	# makes it easier on consumers since each call do this function
	# doesn't have to pass in the filename.
	#
	local file="illumos.sh"
	local variable=$1
	local value=$2

	#
	# Check and ensure the file we need is actually present.
	#
	[ -f "$file" ] || die \
	    "ERROR: illumos nightly environment file '$file' not found."

	#
	# Here is how we determine if there's a default value for the
	# variable that we need to update in-place, or if we can append
	# the new value to the end of the file.
	#
	# Also note, when adding quotes around the value provided, we
	# need to be careful to not use single quotes. The contents of
	# the provided value may reference another shell variable, so we
	# need to make sure variable expansion will occur (it wouldn't
	# if we surrounding the value with single quotes).
	#
	#
	if /usr/bin/grep "^export $variable" "$file" >/dev/null; then
		#
		# If an existing value was found, we assign the new
		# value without modifying the variables location in the
		# file.
		#
		/usr/bin/sed -ie \
		    "s|^export $variable.*|export $variable=\"$value\"|" "$file"
		return $?
	else
		#
		# If a default value wasn't found, we don't need to
		# worry about any references to this variable in the
		# file, so we can simply append the value to the end of
		# the file.
		#
		echo "export $variable=\"$value\"" >> "$file"
		return $?
	fi
}

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

log_must nightly_env_set_var "NIGHTLY_OPTIONS"     "-nlCDprt"
log_must nightly_env_set_var "GATE"                "openzfs-nightly"
log_must nightly_env_set_var "CODEMGR_WS"          "$PWD"
log_must nightly_env_set_var "ON_CLOSED_BINS"      "$PWD/closed"
log_must nightly_env_set_var "ONNV_BUILDNUM"       "151014"
log_must nightly_env_set_var "ENABLE_IPP_PRINTING" "#"
log_must nightly_env_set_var "ENABLE_SMB_PRINTING" "#"
log_must nightly_env_set_var "GCC_ROOT"            "/opt/gcc-4.4.4"
log_must nightly_env_set_var "CW_GCC_DIR"          "\${GCC_ROOT}/bin"
log_must nightly_env_set_var "__GNUC"              ""
log_must nightly_env_set_var "CW_NO_SHADOW"        "1"
log_must nightly_env_set_var "PERL_VERSION"        "5.16.1"
log_must nightly_env_set_var "PERL_ARCH"           "i86pc-solaris-thread-multi-64int"
log_must nightly_env_set_var "PERL_PKGVERS"        "-5161"
log_must nightly_env_set_var "SPRO_ROOT"           "/opt/sunstudio12.1"
log_must nightly_env_set_var "ONLY_LINT_DEFS"      "-I/opt/sunstudio12.1/prod/include/lint"

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
