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
         * This parameter allows for the GitHub repository owner to be
         * overridden when executing this job, providing an easy way
         * change the repository used during the "scm" step of this job.
         * This is especially useful during when testing changes to the
         * Ansible files.
         */
        stringParam('REPO_OWNER', 'openzfs',
            'The GitHub owner used when fetching openzfs-build project.')

        /*
         * In addition to overriding the GitHub owner, it can also be
         * useful to override the branch that is used when fetching the
         * repository; this parameter makes this possible.
         */
        stringParam('REPO_BRANCH', 'master',
            'The Git branch used when fetching the openzfs-build project.')
    }

    steps {
        /*
         * This should be obvious, but we need to checkout the build
         * repository in order to have access to the Ansible playbook
         * that we'll use to stand up a new DCenter instance.
         */
        scm {
            github('$REPO_OWNER/openzfs-build', '$REPO_BRANCH')
        }

        /*
         * Execute the Ansible playbook which will create a new DCenter
         * instance, which will then use the Jenkins swarm plugin to
         * create a new Jenkins slave.
         */
        shell('ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv ' +
            '--extra-vars=' +
            '"properties_path=\'$WORKSPACE/$BUILD_TAG.properties\'" ' +
            'ansible/create-build-slave.yml ' +
            '--vault-password-file /etc/openzfs.conf')

        /*
         * We have to place the "environmentVariables" command inside a
         * flexiblePublish step like we do here, to ensure we load the
         * properties file even if the above "shell" command fails.
         *
         * It's possible for the above command to create a new
         * slave instance, but fail during the configuration step; in
         * this case we still need to load the instance's ID using this
         * properties file, or else the instance could not be destroyed.
         */
        publishers {
            flexiblePublish {
                step {
                    environmentVariables {
                        propertiesFile('$BUILD_TAG.properties')
                    }
                }
            }
        }
    }
}
