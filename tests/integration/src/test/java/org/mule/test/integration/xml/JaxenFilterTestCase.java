/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

public class JaxenFilterTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/xml/jaxen-routing-conf-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/xml/jaxen-routing-conf-flow.xml"}});
    }

    public JaxenFilterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testJaxen() throws Exception
    {
        MuleClient client = muleContext.getClient();
        InputStream po = getClass().getResourceAsStream("/org/mule/test/integration/xml/purchase-order.xml");

        assertNotNull(po);

        MuleMessage msg = new DefaultMuleMessage(po, muleContext);
        MuleMessage res = client.send("vm://in", msg);

        Object payload = res.getPayload();
        assertTrue("payload is of type " + payload.getClass(), payload instanceof Document);
    }
}
