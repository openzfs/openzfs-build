/*
 * Orchestrate a full build and test cycle of OpenZFS, including the
 * creation and destruction of Jenkins slaves to do the heavy lifting.
 */
multiJob("openzfs-regression-tests") {
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

//    parameters {
//        /*
//         * This parameter will usually be set due to this job being
//         * triggered by the GitHub pull request builder, but to allow
//         * this job to be manually run we expose the "sha1" parameter
//         * here.
//         */
//        stringParam('sha1', 'origin/master',
//            'The git commit hash or branch name to build and test.')
//    }
//
//    scm {
//        git {
//            /*
//             * We need to tweak the refspec to ensure we fetch the
//             * commits that belong to pull requests; by default this
//             * would not happen. We don't need to worry about changing
//             * branches here, though, since we don't actual do the build
//             * in this job. Instead, we pass the commit to be built on
//             * to the "openzfs-build-nightly" job that's run below.
//             * Thus, we need to ensure the repository listed here
//             * matches the repository used in the build nightly job.
//             */
//            remote {
//                github('openzfs/openzfs')
//                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
//            }
//        }
//    }
//
//    triggers {
//        pullRequest {
//            /*
//             * Any GitHub user that's included in this list will be
//             * allowed to trigger new builds without approval. If a pull
//             * request is opened by a user that's not in this list, a
//             * member of this list will have to approve the build using
//             * the trigger phrase specified below.
//             */
//            admin(['prakashsurya'])
//
//            /*
//             * We poll the upstream repository once every 5 minutes to
//             * detect new and changes pull requests.
//             */
//            cron('H/5 * * * *')
//
//            /*
//             * If a user not on the whitelist submits a pull request,
//             * testing requires approval before it will start. A comment
//             * of this phrase on the pull request, from a user on the
//             * white list, will grant approval.
//             */
//            triggerPhrase('test this please')
//
//            /*
//             * Allow white listed user to bypass the trigger phrase.
//             */
//            onlyTriggerPhrase(false)
//
//            /*
//             * The Jenkins master is hiding behind a firewall, so we
//             * can't use GitHub hooks and must rely on polling.
//             */
//            useGitHubHooks(false)
//
//            /*
//             * Require the trigger phrase for users not whitelisted.
//             */
//            permitAll(false)
//
//            /*
//             * Don't automatically close pull requests if they fail they
//             * automated testing.
//             */
//            autoCloseFailedPullRequests(false)
//
//            extensions {
//                commitStatus {
//                    context('OpenZFS Regression Tests')
//                    startedStatus('Tests have started.')
//                    triggeredStatus('Tests have been triggered.')
//                    completedStatus('SUCCESS', 'Tests passed.')
//                    completedStatus('FAILURE', 'Tests failed.')
//                }
//            }
//        }
//    }

    steps {
        phase("Create a Jenkins slave to execute the build.") {
            job("create-build-slave") {
                parameters {
                    predefinedProp("SLAVE_NAME", '${BUILD_TAG}')
                    predefinedProp("PROPERTIES_PATH",
                        '${WORKSPACE}/dc_instance.properties')
                }
            }
        }

        /*
         * The "create-build-slave" job above will output a properties
         * file which we need to gain access to the new slave's DCenter
         * instance name.
         *
         * After reading in this properties file, the DCenter instance
         * name will be stored in the DC_INSTANCE_NAME environment
         * variable, and the DNS address of the system will be stored in
         * the DC_INSTANCE_DNS environment variable.
         */
        environmentVariables {
            propertiesFile("dc_instance.properties")
        }

//        phase("Build OpenZFS using the Jenkins slave just created.") {
//            job("openzfs-build-nightly") {
//                /*
//                 * The NODE_NAME parameter ensures the build will run on the new Jenkins
//                 * slave that was created in the previous phase.
//                 */
//                parameters {
//                    /*
//                     * This parameter ensures the build will run on the
//                     * new Jenkins slave that was just created in the
//                     * previous phase.
//                     */
//                    nodeLabel("NODE_NAME", '${BUILD_TAG}')
//
//                    /*
//                     * This parameter ensures we test the actual pull
//                     * request commit, and not the merge commit that
//                     * GitHub automatically creates.
//                     */
//                    predefinedProp("sha1", '${sha1}')
//                }
//            }
//        }

        phase("Clone the Jenkins slave to create new slaves for the tests.") {
            job("clone-build-slave") {
                parameters {
                    predefinedProp("CLONE_INSTANCE_NAME", '${DC_INSTANCE_NAME}')
                    predefinedProp("SLAVE_NAME_A", '${BUILD_TAG}-A')
                    predefinedProp("SLAVE_NAME_B", '${BUILD_TAG}-B')
                    predefinedProp("PROPERTIES_PATH",
                        '${WORKSPACE}/dc_clones.properties')
                }
            }
        }

        /*
         * The "clone-build-slave" job above will generate the following
         * properties file that will include environment variables that
         * describe the new DCenter instances that it creates. The file
         * will contain the following properties that we need:
         * DC_INSTANCE_NAME_A and DC_INSTANCE_NAME_B.
         *
         * The job will create two new DCenter instances, and the
         * instances names for each will be contained in these two
         * environment variables.
         */
        environmentVariables {
            propertiesFile("dc_clones.properties")
        }

        phase("Run OpenZFS ztest in a loop, for a targeted amount of time.") {
            job("openzfs-run-ztest") {
                parameters {
                    nodeLabel("NODE_NAME", '${BUILD_TAG}-A')
                    predefinedProp("ZLOOP_RUN_TIME", "300")
                }
            }

            job("openzfs-run-ztest") {
                parameters {
                    nodeLabel("NODE_NAME", '${BUILD_TAG}-B')
                    predefinedProp("ZLOOP_RUN_TIME", "300")
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
         * occurs, we need to run the "destroy-build-slave" job
         * within the "publishers" context.
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
                                propertiesFile("dc_instance.properties")
                                predefinedProp("DC_INSTANCE_NAME",
                                    '$DC_INSTANCE_NAME')

                                propertiesFile("dc_clones.properties")
                                predefinedProp("DC_INSTANCE_NAME_A",
                                    '$DC_INSTANCE_NAME_A')
                                predefinedProp("DC_INSTANCE_NAME_B",
                                    '$DC_INSTANCE_NAME_B')
                            }
                        }
                    }
                }
            }
        }
    }
}
