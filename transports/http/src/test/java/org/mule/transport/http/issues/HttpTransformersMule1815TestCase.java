/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.StringAppendTestTransformer;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpTransformersMule1815TestCase extends AbstractServiceAndFlowTestCase
{

    public static final String OUTBOUND_MESSAGE = "Test message";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Rule
    public DynamicPort dynamicPort4 = new DynamicPort("port4");
   
    public HttpTransformersMule1815TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-transformers-mule-1815-test-service.xml"},
            {ConfigVariant.FLOW, "http-transformers-mule-1815-test-flow.xml"}
        });
    }      
    
    private MuleMessage sendTo(String uri) throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send(uri, OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        return message;
    }

    /**
     * With no transformer we expect just the modification from the FTC
     *
     * @throws Exception
     */
    @Test
    public void testBase() throws Exception
    {
        assertEquals(OUTBOUND_MESSAGE + " Received", sendTo("base").getPayloadAsString());
    }

    /**
     * Adapted model, which should not apply transformers
     *
     * @throws Exception
     */
    @Test
    public void testAdapted() throws Exception
    {
        assertEquals(OUTBOUND_MESSAGE + " Received", sendTo("adapted").getPayloadAsString());
    }

    /**
     * Change in behaviour: transformers are now always applied as part of inbound flow even if component doesn't invoke them.
     * was: Transformers on the adapted model should be ignored
     *
     * @throws Exception
     */
    @Test
    public void testIgnored() throws Exception
    {
        assertEquals(OUTBOUND_MESSAGE +" transformed" +" transformed 2" + " Received",
                sendTo("ignored").getPayloadAsString());
    }

    /**
     * But transformers on the base model should be applied
     *
     * @throws Exception
     */
    @Test
    public void testInbound() throws Exception
    {
        assertEquals(
            // this reads backwards - innermost is first in chain
            StringAppendTestTransformer.append(" transformed 2",
                StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)) + " Received",
                sendTo("inbound").getPayloadAsString());
    }

}
