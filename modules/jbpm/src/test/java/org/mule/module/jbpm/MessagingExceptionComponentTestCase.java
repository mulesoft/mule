/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jbpm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MessagingExceptionComponentTestCase extends AbstractServiceAndFlowTestCase
{
    public MessagingExceptionComponentTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jbpm-component-functional-test-flow.xml"},
            {ConfigVariant.FLOW, "jbpm-component-functional-test-service.xml"}});
    }

    @Test
    public void testNoException() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://exception", "testNoException", null);

        // Both messages should have been sent.
        assertNotNull(client.request("vm://queueC", 1000));
        assertNotNull(client.request("vm://queueD", 1000));
    }

    @Test
    public void testExceptionInService() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://exception", "testExceptionInService", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertTrue(ExceptionUtils.getRootCause(result.getExceptionPayload().getException()) instanceof FunctionalTestException);

        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));
        assertNull(client.request("vm://queueD", 1000));
    }

    @Test
    public void testExceptionInTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("vm://exception", "testExceptionInTransformer", null);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertTrue(ExceptionUtils.getRootCause(result.getExceptionPayload().getException()) instanceof TransformerException);

        // The first message should have been sent, but not the second one.
        assertNotNull(client.request("vm://queueC", 1000));
        assertNull(client.request("vm://queueD", 1000));
    }
}
