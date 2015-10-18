Introduction
============

This repository holds the configuration information necessary to
configure an Ubuntu 14.04 LTS system into an OpenZFS build server using
Ansible[1].

Quickstart
==========

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

Configuring OpenZFS Jenkins Master
----------------------------------

To configure the OpenZFS Jenkins master using ansible, the following
command can be used:

```
    $ ansible-playbook -i ansible/inventory/development \
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
        a. Set the "Kind" to "Secret text"
        b. Set the "Scope" to "Global"
        c. Copy/Paste the API token into the field labelled "Secret"
        d. Paste "OpenZFS Robot API Token" into the "Description" field
        e. Press the "Add" button to finish adding the token
    5. Paste "OpenZFS Robot" into the "Description" field
    6. Click the "Save" button at the bottom of the page to finish

Now, the initial "seed-job" needs to be created which will be used to
import all of the jobs contained in this repository. To do this, perform
the following steps by navigating through the Jenkins web interface:

    1. Click on the "Create new jobs" button
    2. Create the new "seed-job" Jenkins job:
        a. Enter "seed-job" in the text box labelled "Item name"
        b. Select the "Freestyle project" radio button
        c. Click "OK"
        d. Navigate to the "Source Code Management" section:
            i. Select the "Git" radio button
            ii. Enter the repository's URL
                e.g. https://github.com/openzfs/openzfs-build.git
        e. Navigate to the "Build" section:
            i. Select "Process Job DSLs" in the drop-down menu
            ii. Select the "Look on Filesystem" radio button
            iii. Enter "jobs/*.groovy" in the "DSL Scripts" text area
        f. Click "Save" at the bottom of the page
    3. Run the new "seed-job" to import the jobs in this repository.

In addition, the number of executors on the master node should be
increased from the default of 2, to something like 16 or 32. Each build
will consume 2 executor slots on the master node, and 1 executor slot on
the build slave. The executor slots are used like so:

    - The parent "openzfs-regression-tests" job will consume a single
      executor slot on the master node during the entire run time of the
      build and tests; and during the creation and destruction of the
      build slave, as well.

    - The "create-build-slave" and "destroy-build-slave" will each
      consume a single executor slot on the master node. These slots
      will be consumed and released only during the run time of these
      sub jobs (i.e. after the slave is created or destroyed, the
      executor slot held by these jobs will be released).

    - The "openzfs-build-nightly" job, "openzfs-run-ztest" job, and the
      "openzfs-run-zfs-test" job will each consume a single executor
      slot on a build slave. Each run of these jobs are pinned to a
      specific build slave, so the executor slot this job consumes
      shouldn't become an issue (it consumes the sole executor slot on a
      predetermined build slave created specifically for that job).

Thus, if using the default of 2 executors on the master, only a single
build can be executed at any given time. It it recommended to increase
the number of executors on the master node to allow concurrent builds.

Appendix
--------

 1: http://www.ansible.com/
