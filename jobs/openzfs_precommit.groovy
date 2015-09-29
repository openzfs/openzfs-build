/*
 * Orchestrate a full build of OpenZFS, including the creation of a new
 * build slave to perform the build and the destruction of this build
 * slave after the build finishes.
 */
multiJob("openzfs-precommit") {
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
        phase("Create a Jenkins slave to execute the build.") {
            job("create-build-slave") {
                /*
                 * We pass in a path for the properties such that it
                 * will be written to this parent job's workspace. This
                 * makes it simple to read in the file below.
                 */
                parameters {
                    predefinedProp("PROPERTIES_PATH",
                        '${WORKSPACE}/aws_instance.properties')
                }
            }
        }

        /*
         * The "create-build-slave" above will output a properties
         * file which we need to gain access to the new slave's AWS
         * instance id (which is also the slave's node name). Since we
         * had the prior job write to a file in our workspace, we don't
         * need to do anything fancy to read in this file; we can use
         * the default functionality which is to read from our
         * workspace.
         *
         * After reading in this properties file, the AWS instance id
         * will be stored in the AWS_INSTANCE_ID environment variable.
         * This will be used to ensure the "build-illumos" job will be
         * executed on this new slave; without explicitly specifying the
         * node to run on, it's possible for the job to scheduled on a
         * different slave.
         *
         * Note, the new slave's name is equal to its AWS instance id.
         */
        environmentVariables {
            propertiesFile("aws_instance.properties")
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
                    nodeLabel("NODE_NAME", '$AWS_INSTANCE_ID')
                }
            }
        }

        /*
         * We need to be sure to clean up any AWS instances that were
         * created as a result of this job; and this needs to happen
         * regardless of the status of any of the previous phases and/or
         * jobs.
         *
         * If a phase fails, then none of the later phases in the
         * multi-job will be run. As a result we can't simply perform
         * the clean up logic in a phase at the end of the multi-job (it
         * wouldn't be run on failure). To ensure the clean up always
         * occurs, we need to run the "destroy-build-slave" job within
         * the "publishers" context.
         */
        publishers {
            flexiblePublish {
                step {
                    downstreamParameterized {
                        trigger("destroy-build-slave") {
                            block {
                                failure('FAILURE')
                            }

                            parameters {
                                /*
                                 * I don't know why, but it appears that
                                 * we lose the environment variables
                                 * that we previously read via the
                                 * properties file. As a result, we need
                                 * to re-read this file now; otherwise
                                 * we can't use AWS_INSTANCE_ID.
                                 */
                                propertiesFile("aws_instance.properties")
                                predefinedProp("AWS_INSTANCE_ID",
                                    '$AWS_INSTANCE_ID')
                            }
                        }
                    }
                }
            }
        }
    }
}
