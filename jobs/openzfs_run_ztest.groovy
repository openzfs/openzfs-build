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
        choiceParam('ENABLE_WATCHPOINTS', ['no', 'yes'],
            'Should "ARC watchpoints" be enabled? These can be used to ' +
            'detect illegal modification of frozen arc buffers, but incur ' +
            'a large performance overhead.')
        stringParam('RUN_TIME', '9000',
            'The desired number of seconds to run zloop.')
    }

    steps {
        shell('CI_SH_LIB=/usr/local /usr/local/run-ztest.sh')
    }
}
