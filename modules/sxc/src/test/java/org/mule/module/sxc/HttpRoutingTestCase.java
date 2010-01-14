/*
 * $Id: SxcFilterTestCase.java 12264 2008-07-09 22:24:04Z dandiep $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.sxc;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;

public class HttpRoutingTestCase extends FunctionalTestCase
{
    int finished = 0;

    public void testBasicXPath() throws Exception
    {
        final MuleClient client = new MuleClient();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/purchase-order.xml"), out);

        MuleMessage res = client.send("http://localhost:63081/proxy", out.toByteArray(), null);
        System.out.println(res.getPayloadAsString());
        assertTrue(res.getPayloadAsString().contains("purchaseOrder"));
        assertTrue(res.getPayloadAsString().contains("Alice"));
        
        out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/namespace-purchase-order.xml"), out);
        
        res = client.send("http://localhost:63081/proxy", out.toByteArray(), null);
        System.out.println(res.getPayloadAsString());
        assertTrue(res.getPayloadAsString().contains("purchaseOrder"));
        assertTrue(res.getPayloadAsString().contains("Alice"));
    }

    @Override
    protected String getConfigResources()
    {
        return "http-routing-conf.xml";
    }

}
