/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ClientTransformerTestCase extends FunctionalTestCase
{
    String msg = "<test xmlns=\"http://foo/bar\"> foo </test>";

    
    public void testTransformersOnPayload() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("payloadTransformerClient", msg, null);
        
        MuleMessage result = client.request("vm://in", 3000);
        assertNotNull(result);
        String resString = result.getPayloadAsString();
        System.out.println(resString);
        assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\"><test xmlns=\"http://foo/bar\"> foo </test>") != -1);
    }

    public void testTransformersOnProtocol() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("protocolTransformerClient", msg, null);
        
        MuleMessage result = client.request("vm://in", 3000);
        assertNotNull(result);
        String resString = result.getPayloadAsString();
        System.out.println(resString);
        assertTrue(resString.indexOf("<transformed xmlns=\"http://foo\"><soap:Envelope") != -1);
    }
    
    protected String getConfigResources()
    {
        return "client-transformer-conf.xml";
    }

}