/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.handlers;

import org.mule.api.config.MuleProperties;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DefaultRetryPolicyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
