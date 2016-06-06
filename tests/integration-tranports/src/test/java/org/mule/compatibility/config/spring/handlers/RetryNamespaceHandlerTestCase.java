/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.config.spring.handlers.TestRetryNotifier;
import org.mule.runtime.config.spring.handlers.TestRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.retry.notifiers.ConnectNotifier;
import org.mule.runtime.core.retry.policies.NoRetryPolicyTemplate;
import org.mule.runtime.core.retry.policies.RetryForeverPolicyTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;

import org.junit.Test;

public class RetryNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/handlers/retry-namespace-config.xml";
    }

    @Test
    public void testDefaultConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector1");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof NoRetryPolicyTemplate);
        RetryNotifier rn = rpf.getNotifier();
        assertNotNull(rn);
        assertTrue(rn instanceof ConnectNotifier);
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testSimpleDefaults() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector2");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof SimpleRetryPolicyTemplate);
        assertEquals(SimpleRetryPolicyTemplate.DEFAULT_RETRY_COUNT, ((SimpleRetryPolicyTemplate) rpf).getCount());
        assertEquals(SimpleRetryPolicyTemplate.DEFAULT_FREQUENCY, ((SimpleRetryPolicyTemplate) rpf).getFrequency());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testSimpleConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector3");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof SimpleRetryPolicyTemplate);
        assertEquals(5, ((SimpleRetryPolicyTemplate) rpf).getCount());
        assertEquals(1000, ((SimpleRetryPolicyTemplate) rpf).getFrequency());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testForeverConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector4");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof RetryForeverPolicyTemplate);
        assertEquals(5000, ((RetryForeverPolicyTemplate) rpf).getFrequency());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testCustomConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector5");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof TestRetryPolicyTemplate);
        assertTrue(((TestRetryPolicyTemplate) rpf).isFooBar());
        assertEquals(500, ((TestRetryPolicyTemplate) rpf).getRevolutions());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testConnectNotifierConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector6");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        RetryNotifier rn = rpf.getNotifier();
        assertNotNull(rn);
        assertTrue(rn instanceof ConnectNotifier);
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testCustomNotifierConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector7");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        RetryNotifier rn = rpf.getNotifier();
        assertNotNull(rn);
        assertTrue(rn instanceof TestRetryNotifier);
        assertEquals("red", ((TestRetryNotifier) rn).getColor());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testAsynchronousRetryConfig() throws Exception
    {
        Connector c = muleContext.getRegistry().lookupObject("testConnector8");
        assertNotNull(c);

        RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
        assertNotNull(rpf);
        assertTrue(rpf instanceof AsynchronousRetryTemplate);
        
        rpf = ((AsynchronousRetryTemplate) rpf).getDelegate();
        assertTrue(rpf instanceof SimpleRetryPolicyTemplate);
        assertEquals(5, ((SimpleRetryPolicyTemplate) rpf).getCount());
        assertEquals(1000, ((SimpleRetryPolicyTemplate) rpf).getFrequency());
        
        // Give the asynchronous policy some time to connect in another thread
        Thread.sleep(1000);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
}
