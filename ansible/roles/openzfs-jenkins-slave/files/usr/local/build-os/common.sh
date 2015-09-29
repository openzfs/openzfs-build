#
# Copyright (c) 2012, 2015 by Delphix. All rights reserved.
#

#
# We want the 'die' function to kill the entire job, not just the current shell.
# 'exit 1' by itself will not work in cases were we start a subshell such as:
#
# my_var=$(command_invoked_in_subshell arg1 arg2)
#
# So we set a signal handler for SIGTERM and remember the pid of the top level
# process so that the die function can always send it SIGTERM.
#
export __CI_DIE_TOP_PID=$$
trap "exit 1" TERM

function die
{
	local msg bold norm

	bold="\E[1;31m"
	norm="\E[0m"

	msg=$1
	echo -e "${bold}failed:${norm} $msg" >&2
	kill -s TERM $__CI_DIE_TOP_PID
	exit 1
}

function log
{
	local args bold norm

	bold="\E[1m"
	norm="\E[0m"

	args=()
	for arg in "$@"; do
		args[${#args[*]}]="$arg"
	done

	echo -e "${bold}running:${norm} ${args[*]}" >&2
	"${args[@]}"
	return $?
}

function log_must
{
	local args

	args=()
	for arg in "$@"; do
		args[${#args[*]}]="$arg"
	done

	log "${args[@]}" || die "${args[*]}"
	return 0
}
