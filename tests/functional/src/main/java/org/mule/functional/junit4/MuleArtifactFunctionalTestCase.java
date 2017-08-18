/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4;

import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

/**
 * Base class for mule functional test cases that run tests using class loading isolation. This class will set the default values
 * for testing mule components.
 *
 * @since 4.0
 */
@ArtifactClassLoaderRunnerConfig(
    providedExclusions = {
        "org.mule.tests:*:*:*:*",
        "com.mulesoft.compatibility.tests:*:*:*:*"
    },
    testExclusions = {
        "org.mule.runtime:*:*:*:*",
        "org.mule.modules*:*:*:*:*",
        "org.mule.transports:*:*:*:*",
        "org.mule.mvel:*:*:*:*",
        "org.mule.extensions:*:*:*:*",
        "org.mule.connectors:*:*:*:*",
        "org.mule.tests.plugin:*:*:*:*",
        "com.mulesoft.mule.runtime*:*:*:*:*",
        "com.mulesoft.licm:*:*:*:*"
    },
    testInclusions = {
        "*:*:jar:tests:*",
        "*:*:test-jar:*:*"
    },
    sharedRuntimeLibs = {
        "org.mule.tests:mule-tests-unit"
    })
public abstract class MuleArtifactFunctionalTestCase extends ArtifactFunctionalTestCase {

}
