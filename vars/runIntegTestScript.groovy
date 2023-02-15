/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
void call(Map args = [:]) {
    lib = library(identifier: 'jenkins@main', retriever: legacySCM(scm))

    String jobName = args.jobName ?: 'distribution-build-opensearch'
    def buildManifest = lib.jenkins.BuildManifest.new(readYaml(file: args.buildManifest))

    echo "Start integTest for distribution type: " + buildManifest.getDistribution()

    String buildId = buildManifest.build.id
    echo "Build Id: ${buildId}"

    String artifactRootUrl = buildManifest.getArtifactRootUrl(jobName, buildId)
    echo "Artifact root URL: ${artifactRootUrl}"

    String localPath = args.localPath ?: 'None'
    String paths = generatePaths(buildManifest, artifactRootUrl, localPath)
    echo "Paths: ${paths}"

    String component = args.componentName
    echo "Component: ${component}"

    String switchUser = args.switchUserNonRoot
    echo "Switch User to Non-Root (uid=1000): ${switchUser} "

    switchCommandStart = switchUser.equals('true') ? 'su - `id -un 1000` -c "' : ''
    switchCommandEnd = switchUser.equals('true') ? '"' : ''

    sh("pwd")
    String testCommand = 
    [
        switchCommandStart,
        'pwd &&',
        './test.sh',
        'integ-test',
        "${args.testManifest}",
        "--component ${component}",
        "--test-run-id ${env.BUILD_NUMBER}",
        "--paths ${paths}",
        switchCommandEnd,
    ].join(' ')

    echo "Run command: " + testCommand
    sh(testCommand)
}

String generatePaths(buildManifest, artifactRootUrl, localPath) {
    String name = buildManifest.build.name
    String version = buildManifest.build.version
    String platform = buildManifest.build.platform
    String architecture = buildManifest.build.architecture
    String distribution = buildManifest.build.distribution

    String latestOpenSearchArtifactRootUrl = "https://ci.opensearch.org/ci/dbc/distribution-build-opensearch/${version}/latest/${platform}/${architecture}/${distribution}"
    if (localPath.equals('None')) {
        echo "No localPath found, download from url"
        return name == 'OpenSearch' ?
            "opensearch=${artifactRootUrl}" :
            "opensearch=${latestOpenSearchArtifactRootUrl} opensearch-dashboards=${artifactRootUrl}"
    }
    else {
        echo "User provides localPath, use local artifacts: ${localPath}"
        return name == 'OpenSearch' ?
            "opensearch=${localPath}" :
            "opensearch=${localPath} opensearch-dashboards=${localPath}"
    }
}
