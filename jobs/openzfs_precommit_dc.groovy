/*
 * Orchestrate a full build of OpenZFS, including the creation of a new
 * build slave to perform the build and the destruction of this build
 * slave after the build finishes using DCenter infrastructure.
 */
multiJob("openzfs-precommit-dc") {
    /*
     * This job needs to run on the master node since it relies on
     * the properties file that will be written out by the
     * "create-build-slave" job, and that job can only be run on the
     * master node.
     */
    label "master"

    /*
     * We must explicitly enable concurrent builds, otherwise the
     * default option is to only allow a single build at a time.
     */
    concurrentBuild()

    steps {
        phase("Create a Jenkins slave on DCenter to execute the build.") {
            job("create-dc-build-slave") {
                parameters {
                    predefinedProp("SLAVE_NAME", '${BUILD_TAG}')
                }
            }
        }

        phase("Build OpenZFS using the Jenkins slave just created.") {
            job("openzfs-build-nightly") {
                /*
                 * This ensures the build will run on the new Jenkins
                 * slave that was created in the previous phase.
                 * Otherwise, the build could be scheduled on any
                 * available slave.
                 */
                parameters {
                    nodeLabel("NODE_NAME", '${BUILD_TAG}')
                }
            }
        }

        /*
         * We need to be sure to clean up any DCenter instances that
         * were created as a result of this job; and this needs to
         * happen regardless of the status of any of the previous
         * phases and/or jobs.
         *
         * If a phase fails, then none of the later phases in the
         * multi-job will be run. As a result we can't simply perform
         * the clean up logic in a phase at the end of the multi-job (it
         * wouldn't be run on failure). To ensure the clean up always
         * occurs, we need to run the "destroy-dc-build-slave" job
         * within the "publishers" context.
         */
        publishers {
            flexiblePublish {
                step {
                    downstreamParameterized {
                        trigger("destroy-dc-build-slave") {
                            block {
                                failure('FAILURE')
                            }

                            parameters {
                                /*
                                 * The "create-dc-build-slave" job above
                                 * will create the DCenter instance such
                                 * that it's instance name matches it's
                                 * Jenkins slave name. Thus, that's why
                                 * we're able to use '$BUILD_TAG' here;
                                 * since that was used previously, when
                                 * creating the instance.
                                 */
                                predefinedProp("INSTANCE_NAME", '${BUILD_TAG}')
                            }
                        }
                    }
                }
            }
        }
    }
}