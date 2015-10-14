/*
 * Spin up a DCenter instance which can be used to build OpenZFS.
 */
job("create-build-slave") {
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
         * This specifies the name that will be assigned to the new
         * slave that is created. By exposing this as a parameter, a
         * parent job can specify the new slave's name, and can then
         * user this name to pin jobs to this specific slave.
         */
        stringParam('SLAVE_NAME', null,
            'The name that will be assigned to the newly created slave.')

        /*
         * This specifies the full path of where the Ansible playbook
         * should write the properties file. This properties file is
         * needed to expose the DCenter instance name of the new slave
         * to the parent job, so the instance name can be used to
         * destroy the slave after it's work is completed.
         */
        stringParam('PROPERTIES_PATH', null,
            'Full path of where to write the properties file')
    }

    steps {
        /*
         * This should be obvious, but we need to checkout the build
         * repository in order to have access to the Ansible playbook
         * that we'll use to stand up a new DCenter instance.
         */
        scm {
            github("prakashsurya/openzfs-build", "master")
        }

        /*
         * Execute the Ansible playbook which will create a new DCenter
         * instance, which will then use the Jenkins swarm plugin to
         * create a new Jenkins slave.
         */
        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "--extra-vars=\"jenkins_name='\$SLAVE_NAME'\" " +
            "--extra-vars=\"properties_path='\$PROPERTIES_PATH'\" " +
            "ansible/create-build-slave.yml " +
            "--vault-password-file /etc/openzfs.conf")
    }
}
