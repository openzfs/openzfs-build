/*
 * Destroy a previously created AWS instance (e.g. a prior build slave).
 */
job("destroy-build-slave") {
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
         * This parameter specifies the AWS instances id that will be
         * destroyed using the Ansible playbook below. If the instance
         * doesn't exist, this job will still return success.
         */
        stringParam('AWS_INSTANCE_ID', null, "AWS instance id")
    }

    steps {
        /*
         * We need to checkout the build repository in order to have
         * access to the Ansible playbook, which is used to interact
         * with Amazon Web Services.
         */
        scm {
            github("prakashsurya/openzfs-build", "master")
        }

        /*
         * Execute the Ansible playbook which will attempt to destroy
         * the AWS instance specified by the AWS_INSTANCE_ID parameter.
         */
        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "--extra-vars=\"aws_instance_id='\$AWS_INSTANCE_ID'\" " +
            "ansible/destroy-build-slave.yml " +
            "--vault-password-file /etc/openzfs.conf")
    }
}
