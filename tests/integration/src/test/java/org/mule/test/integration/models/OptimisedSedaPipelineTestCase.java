/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.models;

public class OptimisedSedaPipelineTestCase extends AbstractPipelineTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/models/optimised-seda-pipeline-test-config.xml," +
                "org/mule/test/integration/models/pipeline-test-config.xml";
    }
}
