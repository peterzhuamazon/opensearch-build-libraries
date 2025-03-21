/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package jenkins

import org.junit.*
import groovy.json.JsonOutput
import jenkins.ComponentRepoData

class TestComponentRepoData {
    private ComponentRepoData componentRepoData
    private final String metricsUrl = 'http://example.com'
    private final String awsAccessKey = 'testAccessKey'
    private final String awsSecretKey = 'testSecretKey'
    private final String awsSessionToken = 'testSessionToken'
    private final String version = "2.18.0"
    private final String indexName = 'maintainer-inactivity-03-2025'
    private def script

    @Before
    void setUp() {
        script = new Expando()
        script.sh = { Map args ->
            if (args.containsKey("script")) {
                return """
                    {
                      "took": 13,
                      "timed_out": false,
                      "_shards": {
                        "total": 25,
                        "successful": 25,
                        "skipped": 0,
                        "failed": 0
                      },
                      "hits": {
                        "total": {
                          "value": 3045,
                          "relation": "eq"
                        },
                        "max_score": null,
                        "hits": [
                          {
                            "_index": "maintainer-inactivity-03-2025",
                            "_id": "c45967b3-9f0b-3b5d-aaa8-45b36876077b",
                            "_score": null,
                            "_source": {
                              "github_login": "foo"
                            },
                            "fields": {
                              "github_login.keyword": [
                                "foo"
                              ]
                            },
                            "sort": [
                              1740873626925
                            ]
                          },
                          {
                            "_index": "maintainer-inactivity-03-2025",
                            "_id": "2fa5f6a5-fe79-30ab-bb08-7ac837e55a01",
                            "_score": null,
                            "_source": {
                              "github_login": "bar"
                            },
                            "fields": {
                              "github_login.keyword": [
                                "bar"
                              ]
                            },
                            "sort": [
                              1740873626925
                            ]
                          }
                        ]
                      }
                    }
                """
            }
            return ""
        }
        componentRepoData = new ComponentRepoData(metricsUrl, awsAccessKey, awsSecretKey, awsSessionToken, version, indexName, script)
    }

    @Test
    void testGetMaintainersQuery(){
        String expectedOutput = JsonOutput.toJson([
                size   : 100,
                _source: "github_login",
                collapse: [
                        field: "github_login.keyword"
                ],
                query  : [
                        bool: [
                                filter: [
                                        [
                                                match_phrase: [
                                                        "repository.keyword": "sql"
                                                ]
                                        ],
                                        [
                                                match_phrase: [
                                                        "inactive": "true"
                                                ]
                                        ]
                                ]
                        ]
                ],
                sort   : [
                        [
                                current_date: [
                                        order: "desc"
                                ]
                        ]
                ]
        ]).replace('"', '\\"')

        def result = componentRepoData.getMaintainersQuery('sql', true)
        assert result == expectedOutput
    }

    @Test
    void testGetMaintainers(){
        def expectedOutput = ['foo', 'bar']
        def result = componentRepoData.getMaintainers('sql')
        assert  result == expectedOutput
    }

    @Test
    void testGetMaintainersException() {
        script = new Expando()
        script.println = { String message ->
            assert message.startsWith("Error fetching the maintainers for sql:")
        }
        componentRepoData = new ComponentRepoData(metricsUrl, awsAccessKey, awsSecretKey, awsSessionToken, version, indexName, script)
        componentRepoData.openSearchMetricsQuery = [
                fetchMetrics: { query ->
                    throw new RuntimeException("Test exception")
                }
        ]
        def result = componentRepoData.getMaintainers('sql')
        assert result == null
    }
}
