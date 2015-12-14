def repo_owner = 'openzfs'
def repo_branch = 'workflow'

def dc_instance_name = ''
def dc_instance_name_a = ''
def dc_instance_name_b = ''

node('master') {
    try {
        def create = build job: 'create-build-slave', parameters: [[
            $class: 'StringParameterValue',
            name: 'REPO_OWNER',
            value: repo_owner
        ], [
            $class: 'StringParameterValue',
            name: 'REPO_BRANCH',
            value: repo_branch
        ]]

        dc_instance_name = create.rawBuild.envVars.DC_INSTANCE_NAME

        build job: 'openzfs-build-nightly', parameters: [[
            $class: 'StringParameterValue',
            name: 'sha1',
            value: sha1,
        ], [
            $class: 'NodeParameterValue',
            name: 'NODE',
            labels: [dc_instance_name],
            nodeEligibility: [$class: 'AllNodeEligibility']
        ]]


        def clone = build job: 'clone-build-slave', parameters: [[
            $class: 'StringParameterValue',
            name: 'REPO_OWNER',
            value: repo_owner
        ], [
            $class: 'StringParameterValue',
            name: 'REPO_BRANCH',
            value: repo_branch
        ], [
            $class: 'StringParameterValue',
            name: 'CLONE_INSTANCE_NAME',
            value: dc_instance_name
        ]]

        dc_instance_name_a = clone.rawBuild.envVars.DC_INSTANCE_NAME_A
        dc_instance_name_b = clone.rawBuild.envVars.DC_INSTANCE_NAME_B

        parallel "openzfs-run-ztest": {
            build job: 'openzfs-run-ztest', parameters: [[
                $class: 'StringParameterValue',
                name: 'ZLOOP_RUN_TIME',
                value: '9000'
            ], [
                $class: 'NodeParameterValue',
                name: 'NODE',
                labels: [dc_instance_name_a],
                nodeEligibility: [$class: 'AllNodeEligibility']
            ]]

        }, "openzfs-run-zfs-test": {
            build job: 'openzfs-run-zfs-test', parameters: [[
                $class: 'StringParameterValue',
                name: 'RUNFILE',
                value: '/opt/zfs-tests/runfiles/delphix.run'
            ], [
                $class: 'NodeParameterValue',
                name: 'NODE',
                labels: [dc_instance_name_b],
                nodeEligibility: [$class: 'AllNodeEligibility']
            ]]
        }
    } finally {
        build job: 'destroy-build-slave', parameters: [[
            $class: 'StringParameterValue',
            name: 'REPO_OWNER',
            value: repo_owner
        ], [
            $class: 'StringParameterValue',
            name: 'REPO_BRANCH',
            value: repo_branch
        ], [
            $class: 'StringParameterValue',
            name: 'DC_INSTANCE_NAME',
            value: dc_instance_name
        ], [
            $class: 'StringParameterValue',
            name: 'DC_INSTANCE_NAME_A',
            value: dc_instance_name_a
        ], [
            $class: 'StringParameterValue',
            name: 'DC_INSTANCE_NAME_B',
            value: dc_instance_name_b
        ]]
    }
}
