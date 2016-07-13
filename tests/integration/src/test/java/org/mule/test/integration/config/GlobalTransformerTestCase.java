/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;

import org.junit.Test;

/*
 * This test has been added due to MULE-610
 */
public class GlobalTransformerTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/config/global-transformer-mule-config-flow.xml";
    }

    @Test
    public void testNormal() throws Exception
    {
        MuleMessage msg= flowRunner("Test").withPayload(getTestMuleMessage("HELLO!")).run().getMessage();
        assertTrue(msg.getPayload() instanceof byte[]);
    }
}
