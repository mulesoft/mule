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
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.InputStream;

import org.junit.Test;
import org.w3c.dom.Document;

public class JaxenFilterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/xml/jaxen-routing-conf-flow.xml";
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
