#
# Copyright (c) 2015 by Delphix. All rights reserved.
#

"""
This script deploys one or more Ansible roles to a machine.

You can use this to test new roles that are in development or to deploy roles
to short-lived systems that aren't in any of the version controlled
playbooks.

This must be run from the "ansible" directory of the repository.

For usage information: python bin/deploy_role.py --help
"""

import argparse
import os
import subprocess
import sys
import tempfile


DEFAULT_ANSIBLE_GROUP = 'all'
DEFAULT_KEEP = False


def write_inventory(inventory_fh, host, group):
    """
    :param inventory_fh: A writable file handle to an inventory file
    :param host: Hostname to run against
    :param group: The group name to use in the inventory file. Default is
        `DEFAULT_ANSIBLE_GROUP`
    """
    inventory_fh.write("[{group}]\n".format(group=group))
    inventory_fh.write("{host}\n".format(host=host))
    inventory_fh.flush()


def write_playbook(playbook_fh, roles, group, sudo=False):
    """
    :param playbook_fh: A writable file handle to a playbook file
    :param roles: An ordered list of strings, each an Ansible role name to
        be applied.
    :param group: The group name to use in the playbook. Default is
        `DEFAULT_ANSIBLE_GROUP`.
    :param sudo: Boolean indicating if the playbook should be run with sudo
    privileges
    """
    playbook_fh.write("---\n")
    playbook_fh.write("- hosts: all\n")
    if sudo:
        playbook_fh.write("  sudo: true\n")
    playbook_fh.write("  tasks:\n")
    playbook_fh.write("    - name: Run apt-get update\n")
    playbook_fh.write("      apt:\n")
    playbook_fh.write("        update_cache=yes\n")
    playbook_fh.write("        cache_valid_time=600\n")
    playbook_fh.write("      when: ansible_distribution == 'Ubuntu'\n\n")
    playbook_fh.write("- hosts: {group}\n".format(group=group))
    if sudo:
        playbook_fh.write("  sudo: true\n")
    playbook_fh.write("  roles:\n")
    for role in roles:
        playbook_fh.write("    - {role}\n".format(role=role))
    playbook_fh.flush()


def run_ansible_galaxy():
    repo_root = os.path.join(os.path.dirname(sys.argv[0]), "..")
    cmd = ["ansible-galaxy", "install",
           "-p", "%s" % os.path.join(repo_root, "roles"),
           "-r", "%s" % os.path.join(repo_root, "requirements.yml"),
           "--force"]
    print "running: %s" % " ".join(cmd)
    process = subprocess.Popen(" ".join(cmd), stdout=subprocess.PIPE,
                               shell=True)
    for c in iter(lambda: process.stdout.read(1), ''):
        sys.stdout.write(c)


def run_ansible(user, password, playbook_fh, inventory_fh):

    cmd = ["ANSIBLE_FORCE_COLOR=true",
           "ansible-playbook",
           "-i", "%s" % inventory_fh.name]
    if user and password:
        cmd.append("--extra-vars=\"ansible_ssh_user='%s' "
                   "ansible_ssh_pass='%s'\"" % (user, password))
    cmd += ["%s" % playbook_fh.name, "-vvvv"]
    print "running: %s" % " ".join(cmd)
    process = subprocess.Popen(" ".join(cmd), stdout=subprocess.PIPE,
                               shell=True)
    for c in iter(lambda: process.stdout.read(1), ''):
        sys.stdout.write(c)
    process.wait()
    return process.returncode


def check_rc(returncode):
    if int(returncode) != 0:
        raise Exception("Return code non-zero: %s" % returncode)


def main(host, user, password, roles, group, sudo, keep):
    run_ansible_galaxy()
    with tempfile.NamedTemporaryFile(
        dir=os.getcwd(), suffix='.yml', delete=(not keep)) as playbook_fh, \
            tempfile.NamedTemporaryFile(dir=os.path.join(
                os.getcwd(), 'inventory'), delete=(not keep)) as inventory_fh:
        write_playbook(playbook_fh, roles, group, sudo)
        write_inventory(inventory_fh, host, group)
        check_rc(run_ansible(
            user, password, playbook_fh, inventory_fh))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument('-g', '--group',
                        default=DEFAULT_ANSIBLE_GROUP,
                        help="Ansible host group to use. Useful for causing "
                             "'group_vars' to take effect. "
                             "Default: '%(default)s'",)
    parser.add_argument('-H', '--host',
                        help="Address of host to apply roles to",
                        required=True)
    parser.add_argument('-k', '--keep',
                        default=DEFAULT_KEEP,
                        action='store_true',
                        help="Don't delete the temporary playbook file that "
                             "is created")
    parser.add_argument('-p', '--password',
                        help="Password for the host")
    parser.add_argument('-r', '--roles',
                        help="Comma-separated list of roles to deploy. "
                             "Ex: 'dlpx.ldap' or "
                             "'dlpx.common,dlpx.ldap,dustinbrown.datadog'",
                        required=True,
                        type=lambda x: x.split(','))
    parser.add_argument('-s', '--sudo',
                        action='store_true',
                        help='Apply the roles with sudo')
    parser.add_argument('-u', '--user',
                        help="Username for the host")
    args = parser.parse_args()
    main(args.host, args.user, args.password, args.roles, args.group,
         args.sudo, args.keep)
