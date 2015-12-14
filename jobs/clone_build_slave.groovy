/*
 * Clone an existing build slave into two new Jenkins slaves.
 */
job("clone-build-slave") {
    label 'master'
    concurrentBuild()
    wrappers {
        colorizeOutput()
    }

    parameters {
        stringParam('CLONE_INSTANCE_NAME', null,
            'The DCenter instance name of the instance to clone.')
        stringParam('REPO_OWNER', 'openzfs',
            'The GitHub owner used when fetching openzfs-build project.')
        stringParam('REPO_BRANCH', 'master',
            'The Git branch used when fetching the openzfs-build project.')
    }

    steps {
        scm {
            github('$REPO_OWNER/openzfs-build', '$REPO_BRANCH')
        }

        shell('ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv ' +
            '--extra-vars="clone_name=\'$CLONE_INSTANCE_NAME\'" ' +
            '--extra-vars=' +
            '"properties_path=\'$WORKSPACE/$BUILD_TAG.properties\'" ' +
            'ansible/clone-build-slave.yml ' +
            '--vault-password-file /etc/openzfs.conf')

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
