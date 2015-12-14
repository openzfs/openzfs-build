/*
 * Orchestrate a full build and test cycle of OpenZFS, including the
 * creation and destruction of Jenkins slaves to do the heavy lifting.
 */
workflowJob("openzfs-regression-tests-v2") {
    parameters {
        /*
         * This parameter will usually be set due to this job being
         * triggered by the GitHub pull request builder, but to allow
         * this job to be manually run we expose the "sha1" parameter
         * here.
         */
        stringParam('sha1', 'origin/master',
            'The git commit hash or branch name to build and test.')
    }

    definition {
        cps {
            script(readFileFromWorkspace('workflows/openzfs_regression_tests_v2.groovy'))
        }
    }
}
