/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JmsPropertyScopeTestCase extends AbstractPropertyScopeTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            { ConfigVariant.SERVICE, "org/mule/test/message/jms-property-scope.xml" } ,
            { ConfigVariant.FLOW, "org/mule/test/message/jms-property-scope-flow.xml" }
        });
    }

    public JmsPropertyScopeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    @Test
    public void testRequestResponse() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setOutboundProperty("foo", "fooValue");
        message.setReplyTo("jms://reply");

        client.dispatch("inbound", message);
        MuleMessage result = client.request("jms://reply", 10000);

        assertNotNull(result);
        assertEquals("test bar", result.getPayload());
        assertEquals("fooValue", result.<Object> getInboundProperty("foo"));
    }
}
