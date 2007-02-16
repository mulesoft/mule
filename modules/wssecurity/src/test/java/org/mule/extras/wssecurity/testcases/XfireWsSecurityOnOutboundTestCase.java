
/*
 * $Id$ 
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.testcases;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.Properties;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

public class XfireWsSecurityOnOutboundTestCase extends FunctionalTestCase
{
    public void testGoodUserNameTokenAuthentication () throws Exception
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

        UMOMessage m = client.send("vm://testin", "Test", props);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayload().equals("Test"));
    }

    public void testBadUserNameTokenAuthentication () throws Exception
    {
        MuleClient client = new MuleClient();
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        // Password type : text or digest
        props.setProperty(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        // User name to send
        props.setProperty(WSHandlerConstants.USER, "baduser");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");

        UMOMessage m = null;
        try
        {
            m = client.send("vm://testin", "Test", props);
        }
        catch (Exception e)
        {
            assertNotNull(e);
        }
        assertNull(m);
    }

    public void testGoodUserNameEncrypted () throws Exception
    {
        MuleClient client = new MuleClient();
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        // User name to send
        props.setProperty(WSHandlerConstants.USER, "mulealias");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");
        // Property file containing the Encryption properties
        props.setProperty(WSHandlerConstants.ENC_PROP_FILE, "out-encrypted-security.properties");

        UMOMessage m = client.send("vm://testin", "Test", props);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayload().equals("Test"));
    }

    public void testBadUserNameEncrypted () throws Exception
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

        UMOMessage m = null;
        try
        {
            m = client.send("vm://testin", "Test", props);
        }
        catch (Exception e)
        {
            assertNotNull(e);
        }
        assertNull(m);
    }

    public void testSignedSoapMessage () throws Exception
    {
        MuleClient client = new MuleClient();
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        // User in keystore
        props.setProperty(WSHandlerConstants.USER, "mulealias");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");
        // Configuration for accessing private key in keystore
        props.setProperty(WSHandlerConstants.SIG_PROP_FILE, "out-signed-security.properties");
        // possible values are : "IssuerSerial" ( recommended ) and "DirectReference"
        props.setProperty(WSHandlerConstants.SIG_KEY_ID, "IssuerSerial");

        UMOMessage m = client.send("vm://testin", "Test", props);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        assertTrue(m.getPayload().equals("Test"));
    }

    protected String getConfigResources ()
    {
        return "wssecurity-mule-config-for-outbound.xml";
    }
}