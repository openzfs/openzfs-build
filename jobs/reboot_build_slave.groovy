/*
 * Reboot a DCenter instance that is being used to build/test OpenZFS.
 */
job("reboot-build-slave") {
    /*
     * This job needs to run on the master node since it relies on
     * "ansible-playbook" being available. This utility will only be
     * installed on the master node, so we need to ensure this job is
     * only run on that node.
     */
    label 'master'

    /*
     * We must explicitly enable concurrent builds, otherwise the
     * default option is to only allow a single build at a time.
     */
    concurrentBuild()

    /*
     * When using the "ANSIBLE_FORCE_COLOR=true" environment variable
     * with "ansible-playbook", the tool will emit colorized output
     * which makes it easier to distinguish different "sections" of the
     * tool's output. In order for this color to be visible in the
     * Jenkins job's console page, we need explicitly enable the option.
     */
    wrappers {
        colorizeOutput()
    }

    parameters {
        /*
         * This specifies the name that will be assigned to the build
         * slave after it is rebooted. This may, or may not, be the same
         * slave name the system has prior to the reboot operation.
         */
        stringParam('SLAVE_NAME', null,
            'The post-reboot name that will be assigned to the build slave.')

        /*
         * This parameter specifies the DNS address of the DCenter
         * instance that we will attempt to reboot.
         */
        stringParam('DC_INSTANCE_DNS', null,
            'The DNS address of the DCenter instance to be rebooted.')
    }

    steps {
        /*
         * This should be obvious, but we need to checkout the build
         * repository in order to have access to the Ansible playbook
         * that we'll use to stand up a new DCenter instance.
         */
        scm {
            github("prakashsurya/openzfs-build", "testing")
        }

        /*
         * Execute the Ansible playbook which will reboot the DCenter
         * instance. After the slave boots back up, the Jenkins Swarm
         * plugin will be used to connect it back to the Jenkins master
         * just like what happens when a new slave is created.
         */
        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "--extra-vars=\"jenkins_name='\$SLAVE_NAME'\" " +
            "--extra-vars=\"instance_dns='\$DC_INSTANCE_DNS'\" " +
            "ansible/reboot-build-slave.yml " +
            "--vault-password-file /etc/openzfs.conf")
    }
}
