/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.models;

import org.junit.Ignore;

@Ignore("MULE-2741")
public class OptimisedSedaPipelineTestCase extends AbstractPipelineTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            "org/mule/test/integration/models/optimised-seda-pipeline-test-config.xml",
            "org/mule/test/integration/models/pipeline-test-config.xml"
        };
    }
}
