/*
 * Run OpenZFS's ztest utility in a loop for a set amount of time.
 */
job("openzfs-run-ztest") {
    label 'openzfs-build-slave'
    concurrentBuild()
    wrappers {
        colorizeOutput()
    }

    parameters {
        stringParam('ZLOOP_RUN_TIME', "9000",
            'Target number of seconds to run ztest continuously.')
    }

    steps {
        shell('CI_SH_LIB=/usr/local /usr/local/run-ztest.sh')
    }
}
