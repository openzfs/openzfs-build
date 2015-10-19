/*
 * Synchronize the public Jenkins mirror with the files from this master.
 */
job("synchronize-jenkins-mirror") {
    label 'master'
    wrappers {
        colorizeOutput()
    }

    triggers {
        cron('H * * * *')
    }

    steps {
        scm {
            github("openzfs/openzfs-build", "master")
        }

        shell("ANSIBLE_FORCE_COLOR=true /usr/bin/ansible-playbook -vvvv " +
            "ansible/synchronize-jenkins-mirror.yml")
    }
}
