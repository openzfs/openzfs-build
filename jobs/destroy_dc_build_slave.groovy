/*
 * Destroy a previously created DCenter instance (e.g. a prior build slave).
 */
job("destroy-dc-build-slave") {
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
         * This parameter specifies the DCenter instances name that will
         * be destroyed using the Ansible playbook below.
         */
        stringParam('INSTANCE_NAME', null, "AWS instance id")
    }

    steps {
        /*
         * We need to checkout the build repository in order to have
         * access to the Ansible playbook, which is used to interact
         * with DCenter.
         */
        scm {
            github("prakashsurya/openzfs-build", "master")
        }

        /*
         * Execute the Ansible playbook which will attempt to destroy
         * the DCenter instance specified by the INSTANCE_NAME parameter.
         */
        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "--extra-vars=\"instance_name='\$INSTANCE_NAME'\" " +
            "ansible/destroy-dc-build-slave.yml " +
            "--vault-password-file /etc/openzfs.conf")
    }
}
