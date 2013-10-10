/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.StringAppendTestTransformer;

public class TransformerAttributeTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String OUTBOUND_MESSAGE = "Test message";

    public TransformerAttributeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/transformer-attribute-test-service.xml"},
            {ConfigVariant.FLOW, "vm/transformer-attribute-test-flow.xml"}
        });
    }

    @Test
    public void testSimple() throws Exception
    {
        MuleMessage message = new MuleClient(muleContext).send("vm://simple", OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        assertEquals(StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)  + " Received",
                message.getPayloadAsString());
    }

    @Test
    public void testThrough() throws Exception
    {
        MuleMessage message = new MuleClient(muleContext).send("vm://chained", OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        assertEquals(StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)  + " Received",
                message.getPayloadAsString());
    }

}
