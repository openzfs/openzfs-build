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
        stringParam('SLAVE_NAME_A', null,
            'The Jenkins slave name for one of the new cloned slaves.')
        stringParam('SLAVE_NAME_B', null,
            'The Jenkins slave name for the other newly cloned slave.')
        stringParam('PROPERTIES_PATH', null,
            'Full path of where to write the properties file')
    }


    steps {
        scm {
            github("openzfs/openzfs-build", "master")
        }

        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "--extra-vars=\"clone_name='\$CLONE_INSTANCE_NAME'\" " +
            "--extra-vars=\"jenkins_name_a='\$SLAVE_NAME_A'\" " +
            "--extra-vars=\"jenkins_name_b='\$SLAVE_NAME_B'\" " +
            "--extra-vars=\"properties_path='\$PROPERTIES_PATH'\" " +
            "ansible/clone-build-slave.yml " +
            "--vault-password-file /etc/openzfs.conf")
    }
}
