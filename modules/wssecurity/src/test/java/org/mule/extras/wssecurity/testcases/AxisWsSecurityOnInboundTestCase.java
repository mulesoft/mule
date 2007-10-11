/*
 * $Id: 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.testcases;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.Properties;

import org.apache.ws.security.handler.WSHandlerConstants;

public class AxisWsSecurityOnInboundTestCase extends FunctionalTestCase
{

    public void testGoodUserNameEncrypted() throws Exception
    {
        MuleClient client = new MuleClient(managementContext);
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        // User name to send
        props.setProperty(WSHandlerConstants.USER, "mulealias");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");
        // Property file containing the Encryption properties
        props.setProperty(WSHandlerConstants.ENC_PROP_FILE, "out-encrypted-security.properties");

        UMOMessage m = client.send("axis:http://localhost:64282/MySecuredUMO?method=echo", "Test", props);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayload().equals("Test"));
    }


    public void testBadUserNameEncrypted() throws Exception
    {
        MuleClient client = new MuleClient();
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        // User name to send
        props.setProperty(WSHandlerConstants.USER, "myBadAlias");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");
        // Property file containing the Encryption properties
        props.setProperty(WSHandlerConstants.ENC_PROP_FILE, "out-encrypted-security.properties");

        try
        {
            client.send("axis:http://localhost:64282/MySecuredUMO?method=echo", "Test", props);
            fail("Expected exception");
        }
        catch (Exception e)
        {
            assertNotNull(e);
        }
    }

    protected String getConfigResources()
    {
        return "axis-wssecurity-mule-config.xml";
    }
}