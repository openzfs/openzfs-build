#
# Copyright (c) 2012 by Delphix.
# All rights reserved.
#

#
# The functions in this library are useful for any scripts using the illumos
# nightly script and its corresponding environment files (e.g. the os-gate
# and the installer-gate).
#

source "$CI_SH_LIB/common.sh"

#
# Updates a given nightly environment file. If the given variable is already
# defined in the file it is not set unless the -f option is specified. If
# an existing variable definition is updated it is updated in-place (so any
# later variable declarations that reference that variable are still valid),
# if it is a new definition is appended to the end of the file.
#
function nightly_env_set_var
{
	local OPTARG OPTIND opt force envfile varname varval

	force=false
	while getopts "f" opt; do
		case $opt in
		    f)
			force=true
			;;
		    *)
			echo "Invalid option: -$OPTARG" >&2
			exit 2
			;;
		esac
	done
	shift $((OPTIND-1))

	envfile=$1
	varname=$2
	varval=$3

	if grep "export $varname" "$envfile" >/dev/null; then
		if $force; then
			sed -ie "s@#\?export $varname.*@export $varname=\"$varval\"@" "$envfile"
		else
			echo "NOTE: Not setting $varname, value is already set in $envfile."
		fi
		return $?
	else
		echo "export $varname=\"$varval\"" >>"$envfile"
		return $?
	fi
}

function __echo_jenkins_helper_script
{
	local build_mail

	build_mail=$1

	cat <<EOF
#!/bin/bash
#
# Files created by nightly.sh
#
build_time_file="\$TMPDIR/build_time"
build_env_file="\$TMPDIR/build_environ"
mail_msg_file="\$TMPDIR/mail_msg"

cat \$build_time \$build_environ_file \$mail_msg_file >"$build_mail"
EOF
	return 0
}

#
# Runs the nightly script on the given environment file. The path to the nightly
# script to run must be specified along with the directory in which the build
# will take place. At the end of the build a file called 'build.mail' will be
# placed in the build_dir that is suitable for mailing to anyone interested in
# the results of the build. The exit return status of this function is the same
# as the exit status of the nightly script.
#
function nightly_run
{
	local nightly
	local build_dir
	local envfile

	nightly=$1
	build_dir=$2
	envfile=$3

	local build_pid
	local jenkins_helper
	local lockname
	local nightly_pid
	local tail_pid
	local nightly_tmpdir
	local result

	build_pid=$$

	#
	# We do not want to let the nightly script send mail, but we do want
	# to mimic the mail_msg sent by the nightly script. Rather than try
	# to find the mail_msg file in the log directory once the build is
	# finished we use the POST_BUILD hook in the nighly script to assemble
	# our own copy of the mail message in the build directory.
	#
	jenkins_helper="$build_dir/jenkins_helper.sh"
	log_must __echo_jenkins_helper_script "$build_dir/mail_msg" >"$jenkins_helper"
	log_must chmod +x "$jenkins_helper"

	#
	# The LOCKNAME defined in the environment file should normally not
	# be changed, but we set it to a predicatble value because it is
	# actually a symlink to a file whose name ends with the PID of
	# the build process, which we use to identify the build's temp
	# directory later on.
	#
	lockname="jenkins-$build_pid-nightly.lock"

	log_must nightly_env_set_var -f "$envfile" "LOCKNAME" "$lockname"
	log_must nightly_env_set_var -f "$envfile" "POST_NIGHTLY" "$jenkins_helper"

	log env -i time "$nightly" "$envfile" &
	nightly_pid=$!

	sleep 10
	nightly_tmpdir=/tmp/nightly.tmpdir.$(readlink -f "/tmp/$lockname" | \
	    sed -E 's@.*\.([0-9]+)@\1@') || die "could not look up tmpdir"
	tail -f $nightly_tmpdir/mail_msg &
	tail_pid=$!

	wait $nightly_pid
	result=$?
	kill $tail_pid

	return $result
}

#
# Checks the section of the mail_msg file in the given build directory (assumes
# mail_msg was created by nightly_run() function). If the section between the given
# lines is empty returns 0, otherwise returns non-zero. If the start line does not
# exist always returns 0.
#
function mail_msg_is_clean
{
	local build_dir start end

	build_dir="$1"
	start="==== $(echo "$2" | sed "s@/@\\\\/@") ===="
	end="==== $(echo "$3" | sed "s@/@\\\\/@") ===="

	if [[ ! -z "$(sed -n "/^$start\$/,/^$end\$/p" $build_dir/mail_msg | sed -e "/^$start\$/d" -e "/^$end\$/d" -e "/^$/d")" ]]; then
		return 1
	fi
	return 0
}
