/*
 * Run OpenZFS's suite of functional regression tests.
 */
job("openzfs-run-zfs-test") {
    label 'openzfs-build-slave'
    concurrentBuild()
    wrappers {
        colorizeOutput()
    }

    parameters {
        stringParam('RUNFILE', '/opt/zfs-tests/runfiles/delphix.run',
            'The run file to use, which lists the specific tests to execute.')
    }

    steps {
        shell('CI_SH_LIB=/usr/local /usr/local/run-zfs-test.sh')
    }
}
