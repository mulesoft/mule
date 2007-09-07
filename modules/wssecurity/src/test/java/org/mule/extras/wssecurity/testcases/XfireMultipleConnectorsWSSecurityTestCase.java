/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.testcases;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.Properties;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

public class XfireMultipleConnectorsWSSecurityTestCase extends FunctionalTestCase
{
    
    public XfireMultipleConnectorsWSSecurityTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }
    
    public void testSecured() throws Exception
    {
        MuleClient client = new MuleClient();
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        // Password type : text or digest
        props.setProperty(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        // User name to send
        props.setProperty(WSHandlerConstants.USER, "gooduser");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");

        UMOMessage m = client.send("vm://secured", "Test", props);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayload().equals("Test"));
    }
    
    public void testUnsecured() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("vm://unsecured", new MuleMessage("Test"));
        
        assertEquals("Test", reply.getPayloadAsString());
    }

    protected String getConfigResources()
    {
        return "xfire-multiple-connectors.xml";
    }

}


