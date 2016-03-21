Introduction
============

This repository holds the configuration information necessary to
configure an Ubuntu[1] 14.04 LTS system into an OpenZFS[2] build server
using Ansible[3], OmniOS[4], Jenkins[5], and internal Delphix[6]
hardware resources. All of the Jenkins job  configuration and Ansible
roles that are necessary for the OpenZFS Jenkins master instance to
operate should be included in this repository, including instructions on
any out-of-band manual configuration that may be necessary.

Overview of OpenZFS Regression Tests
====================================

The OpenZFS Regression Tests are comprised of the following Jenkins jobs
that each perform a specific task within the pipeline. An overview of
each job, and the specific task that it performs is below.

The `openzfs-regression-tests` Jenkins Job
------------------------------------------

This job server as the coordinator of all the sub jobs, shepherding a
given "OpenZFS regression test run" through pipeline. In addition, this
job is responsible for polling for open pull requests, starting
regression test runs to test the pull request, and reporting status
updates to the pull request using GitHub's commit status API (e.g.
posting "pending", "success", and/or "failure" status updates for the
pull request). If any one of the sub jobs fail, the whole regression
test run is deemed a failure; thus all jobs must succeed for this parent
job to be deemed successful (i.e. for the regression test as a whole to
be successful, all sub jobs must be successful).

At a high level, the hierarchy of sub-jobs and phases of this parent job
can be represented by the following diagram:


                  +------------------------------------+
                  | openzfs-regression-tests triggered |
                  +------------------------------------+
                                    |
                                    V
                          +--------------------+
                          | create-build-slave |
                          +--------------------+
                                    |
                                    V
                        +-----------------------+
                        | openzfs-build-nightly |
                        +-----------------------+
                                    |
                                    V
                          +-------------------+
                   +----- | clone-build-slave | ----+
                   |      +-------------------+     |
                   V                                V
        +----------------------+         +-------------------+
        | openzfs-run-zfs-test |         | openzfs-run-ztest |
        +----------------------+         +-------------------+
                   |                                |
                   |     +---------------------+    |
                   +---> | destroy-build-slave | <--+
                         +---------------------+
                                    |
                                    V
                   +---------------------------------+
                   | openzfs-regression-tests result |
                   +---------------------------------+


The `create-build-slave` Jenkins Job
------------------------------------

This job's task is to create a new VM to run the build of OpenZFS from
source, and produce the necessary build products such that the VM can be
upgraded to this build products prior to running the various regression
tests. The VM that is created is based on a standard OmniOS r151014
installation, with a couple minimal configuration changes to the base
image to get it into a usable state; this includes setting a root
password, enabling root login via ssh, and enabling DHCP networking. Any
additional configuration of the system (e.g. installing compiler
packages) is not baked into the base VM, instead this additional
configuration happens in the context of this job (see the
`initialize-omnios` Ansible role for more details).

The `openzfs-build-nightly` Jenkins Job
---------------------------------------

This job's task is to perform a full nightly build of OpenZFS, using the
VM created in the `create-build-slave` job, and also perform an upgrade
of the system using the created build products (i.e. `onu` the system on
which the build occurred). The build occurs by running the `build-os.sh`
script that is installed in `/usr/local` on the build slave, so I'll
defer to that file for the specifics on how the build occurs (see the
`build-os.sh` script of the `openzfs-jenkins-slave` Ansible role).

Additionally, upon a successful build of OpenZFS, the build slave will
be upgraded using the build products created during the build. This
upgrade is performed by executing the `install-os.sh` that's installed
in `/usr/local` on the build slave, which is essentially a very thin
wrapper around the `onu` command which does the upgrade operation (see
the `install-os.sh` script of the `openzfs-jenkins-slave` Ansible role
for more details).

NOTE: The `-l` option has been temporarily removed from from
`NIGHTLY_OPTIONS` due to lint errors being reported and failing
otherwise good builds. The failures are not yet understood, so they are
being suppressed for the time being. This is not intended to be a long
term fix; once the failures are investigated and/or fixed, this option
will be re-enabled.

The `clone-build-slave` Jenkins Job
-----------------------------------

This job's task is to clone the previously upgraded build slave into 2
new VMs, such that they can be used to run the regression tests. Since
the `openzfs-build-nightly` job upgrades the build slave upon a
successful build, cloning the build slave will create another system
that is running the new build products. This way, we can create multiple
slaves running the upgraded build products quickly, and then use these
new slave to run the various regression tests concurrently. Currently,
this job will take the original upgraded build slave (the one created in
`create-build-slave` and upgraded in `openzfs-build-nightly`) and
generate two clones of the system which are used in the next step of the
pipeline (used in the `openzfs-run-ztest` and `openzfs-run-zfs-test`
jobs).

The `openzfs-run-ztest` Jenkins Job
-----------------------------------

This job's task is to run the `ztest` utility continuously, using
"random" parameters, for a specified amount of time (by default it is
run for 9000 seconds, or 2.5 hours). To accomplish this, the
`run-ztest.sh` script is executed which acts as a simple wrapper around
the `zloop.sh` script; both scripts get installed into `/usr/local` on
the build slave. For more details about how these scripts are executed,
or how they are used to invoke the `ztest` utility, see the
`openzfs-build-slave` Ansible role.

The `openzfs-run-zfs-test` Jenkins Job
--------------------------------------

This job's task is to run the OpenZFS regression test suite. To do this,
it executes the `run-zfs-test.sh` script that is installed in
`/usr/local` on the build slave; this script is a very simple wrapper
which in turn executes the `/opt/zfs-tests/bin/zfstest` command to
actually run the OpenZFS test suite. See the `openzfs-jenkins-slave` for
more details on exactly how the test suite is invoked

The `destroy-build-slave` Jenkins Job
-------------------------------------

This job's task is to destroy (or unregister) all of the VMs that were
used in the prior jobs; this includes the build slave that was used to
perform the build of OpenZFS, and also the clones slaves that were used
to execute the regression tests of OpenZFS. This job relies on the
`destroy-build-slave.yml` Ansible playbook to do the heavy lifting
involved with actually performing the destroy operation of the VMs.

Creating a new OpenZFS Jenkins master
=====================================

Dependencies
------------

To start, ensure ansible is installed on the system from which
`ansible-playbook` will be run. See the ansible installation docs for
more information; this process will vary based on the operating system:
http://docs.ansible.com/ansible/intro_installation.html

Once the base `ansible` package is installed, the dependent `ansible`
roles need to be installed as well. This can be done using the following
command:

```
    $ ansible-galaxy install -r ansible/requirements.yml
```

This should download and install the necessary dependencies, and make
them available for use in later `ansible-playbook` commands.

Configuring the OpenZFS Jenkins Master
--------------------------------------

To configure the OpenZFS Jenkins master using ansible, the following
command can be used:

```
    $ ansible-playbook -i ansible/inventory/public-and-private \
        ansible/openzfs-jenkins-master.yml --ask-vault-pass
```
This will prompt for the vault password, which is needed to decrypt any
encrypted files, and then interact with the system listed in the
`development` file to configure it with all necessary packages for it to
function as the OpenZFS Jenkins master.

Once the Jenkins server is configured using the Ansible playbook above,
the GitHub pull request builder plugin needs to be manually configured
with the OpenZFS, "zettabot", GitHub API token. If a new API token needs
to be created, see the following for instructions on that:

https://help.github.com/articles/creating-an-access-token-for-command-line-use/

Once the API token is known, the GitHub pull request plugin can be
configured to use this token to push the results of build and test jobs
to the OpenZFS pull requests. To configure the plugin, perform the
following steps:

1. Click on the "Manage Jenkins" link in the left side bar
2. Click on the "Configure System" link at the top of the list
3. Scroll down to the "GitHub Pull Request Builder" section
4. Click the "Add" button next to the "Credentials" label
  1. Set the "Kind" to "Secret text"
  2. Set the "Scope" to "Global"
  3. Copy/Paste the API token into the field labelled "Secret"
  4. Paste "OpenZFS Robot API Token" into the "Description" field
  5. Press the "Add" button to finish adding the token
5. Paste "OpenZFS Robot" into the "Description" field
6. Click the "Save" button at the bottom of the page to finish

Now, the initial "seed-job" needs to be created which will be used to
import all of the jobs contained in this repository. To do this, perform
the following steps by navigating through the Jenkins web interface:

1. Click on the "Create new jobs" button
2. Create the new "seed-job" Jenkins job:
  1. Enter "seed-job" in the text box labelled "Item name"
  2. Select the "Freestyle project" radio button
  3. Click "OK"
  4. Navigate to the "Source Code Management" section:
    1. Select the "Git" radio button
    2. Enter the repository's URL
        e.g. https://github.com/openzfs/openzfs-build.git
  5. Navigate to the "Build" section:
    1. Select "Process Job DSLs" in the drop-down menu
    2. Select the "Look on Filesystem" radio button
    3. Enter "jobs/*.groovy" in the "DSL Scripts" text area
  6. Click "Save" at the bottom of the page
3. Run the new "seed-job" to import the jobs in this repository.

In addition, the number of executors on the master node should be
increased from the default of 2, to something like 8 or 12 since the
parent "openzfs-regression-tests" job will allow up to 4 concurrent
jobs to run. Each build will consume 2 executor slots on the master
node, and 1 executor slot on the build slave. The executor slots are
used like so:

- The parent "openzfs-regression-tests" job will consume a single
  executor slot on the master node during the entire run time of the
  build and tests; and during the creation and destruction of the
  build slaves, as well.

- The "create-build-slave", "destroy-build-slave", and
  "clone-build-slave" jobs will each consume a single executor slot
  on the master node. These slots will be consumed and released only
  during the run time of these sub jobs (i.e. after the slave is
  created, destroyed, or cloned, the master nodes' executor slot held
  by these jobs will be released).

- The "openzfs-build-nightly" job, "openzfs-run-ztest" job, and the
  "openzfs-run-zfs-test" job will each consume a single executor
  slot on a build slave. Each run of these jobs are pinned to a
  specific build slave, so the executor slot this job consumes
  shouldn't become an issue (it consumes the sole executor slot on a
  predetermined build slave created specifically for that job).

Thus, if using the default of 2 executors on the master, only a single
build can be executed at any given time and it's even possible to get
into a deadlock situation where a "destroy-build-slave" needs to run but
can't allocate an executor slot on the master. It it recommended to
increase the number of executors on the master node to allow concurrent
builds and prevent this deadlock scenario.

Appendix
========

 1. http://www.ubuntu.com/
 2. http://open-zfs.org/
 3. http://www.ansible.com/
 4. http://omnios.omniti.com/
 5. https://jenkins-ci.org/
 6. http://www.delphix.com/
