/*
 * Spin up an AWS instance which can be used to build OpenZFS.
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
         * This specifies the full path of where the Ansible playbook
         * should write the properties file. By exposing this as a
         * parameter, a parent job can provide a path to a file in its
         * workspace which makes it easy for that parent job to read in
         * the properties file. This properties file is needed to expose
         * the AWS instance id of the new slave to a given Jenkins job,
         * which can then be used to pin specific jobs to run on this
         * new slave (the slave's name is equal to its AWS instance id).
         */
        stringParam('PROPERTIES_PATH', null,
            'Full path of where to write the properties file')
    }

    steps {
        /*
         * This should be obvious, but we need to checkout the build
         * repository in order to have access to the Ansible playbook
         * that we'll use to stand up a new AWS instance for the build.
         */
        scm {
            github("prakashsurya/openzfs-build", "master")
        }

        /*
         * Execute the Ansible playbook which will create a new AWS
         * instance, which will then use the Jenkins swarm plugin to
         * create a new Jenkins slave.
         *
         * Also note, we pass in the $PROPERTIES_PATH parameter so the
         * Ansible playbook will write the properties file to the
         * correct location.
         */
        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "--extra-vars=\"properties_path='\$PROPERTIES_PATH'\" " +
            "ansible/create-build-slave.yml " +
            "--vault-password-file /etc/openzfs.conf")
    }
}
