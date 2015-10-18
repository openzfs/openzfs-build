/*
 * Destroy a previously created DCenter instance (e.g. a prior build slave).
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
         * This parameter specifies the DCenter instance name that will
         * be destroyed using the Ansible playbook below.
         */
        stringParam('DC_INSTANCE_NAME', null, "DCenter instance name")
        stringParam('DC_INSTANCE_NAME_A', null, "DCenter instance name")
        stringParam('DC_INSTANCE_NAME_B', null, "DCenter instance name")

        /*
         * This parameter is used to determine if the DCenter instance
         * should be destroyed, or if the instance should only be
         * unregistered. By default, a full destroy is issued.
         */
        stringParam('UNREGISTER_ONLY', 'yes',
            "If 'yes', the instance will be unregistered and not destroyed.")
    }

    steps {
        /*
         * We need to checkout the build repository in order to have
         * access to the Ansible playbook, which is used to interact
         * with DCenter.
         */
        scm {
            github("openzfs/openzfs-build", "master")
        }

        steps {
            shell("ANSIBLE_FORCE_COLOR=true " +
                "/usr/bin/ansible-playbook -vvvv " +
                "--extra-vars=\"unregister_only='\$UNREGISTER_ONLY'\" " +
                "--extra-vars=\"instance_name='\$DC_INSTANCE_NAME_A'\" " +
                "ansible/destroy-build-slave.yml " +
                "--vault-password-file /etc/openzfs.conf")

            shell("ANSIBLE_FORCE_COLOR=true " +
                "/usr/bin/ansible-playbook -vvvv " +
                "--extra-vars=\"unregister_only='\$UNREGISTER_ONLY'\" " +
                "--extra-vars=\"instance_name='\$DC_INSTANCE_NAME_B'\" " +
                "ansible/destroy-build-slave.yml " +
                "--vault-password-file /etc/openzfs.conf")

            shell("ANSIBLE_FORCE_COLOR=true " +
                "/usr/bin/ansible-playbook -vvvv " +
                "--extra-vars=\"unregister_only='\$UNREGISTER_ONLY'\" " +
                "--extra-vars=\"instance_name='\$DC_INSTANCE_NAME'\" " +
                "ansible/destroy-build-slave.yml " +
                "--vault-password-file /etc/openzfs.conf")
        }
    }
}
