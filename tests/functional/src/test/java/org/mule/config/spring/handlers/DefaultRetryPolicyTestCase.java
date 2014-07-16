/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.config.MuleProperties;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class DefaultRetryPolicyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/handlers/default-retry-policy.xml";
    }

    @Test
    public void testPolicyRegistration() throws Exception
    {
        Object obj = muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
        assertNotNull(obj);
        assertTrue(obj.getClass().getName(), obj instanceof SimpleRetryPolicyTemplate);
        assertEquals(3, ((SimpleRetryPolicyTemplate) obj).getCount());
    }

    @Test
    public void testConnectorPolicy() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupConnector("testConnector");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof SimpleRetryPolicyTemplate);
        assertEquals(3, ((SimpleRetryPolicyTemplate) rpf).getCount());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
}
