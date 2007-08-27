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
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.Properties;

import org.apache.ws.security.handler.WSHandlerConstants;

public class AxisMultipleConnectorsWSSecurityTestCase extends FunctionalTestCase
{
    
    public AxisMultipleConnectorsWSSecurityTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }
    
    public void testAxisAuthentication() throws Exception
    {
        // Action to perform : Signature
        Properties props = new Properties();

        // Action to perform : user token
        props.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        // User name to send
        props.setProperty(WSHandlerConstants.USER, "mulealias");
        // Callback used to retrive password for given user.
        props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mule.extras.wssecurity.callbackhandlers.MuleWsSecurityCallbackHandler");
        // Property file containing the Encryption properties
        props.setProperty(WSHandlerConstants.ENC_PROP_FILE, "out-encrypted-security.properties");

        // Set signature method to DirectReference
        props.setProperty(WSHandlerConstants.SIG_KEY_ID, "DirectReference");

        props.setProperty(WSHandlerConstants.USE_REQ_SIG_CERT, "false");

        UMOMessage result;

        MuleClient client = new MuleClient();
        result = client.send("vm://secured", "Inputgot", props);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayloadAsString().equalsIgnoreCase("inputgot"));
        System.out.println("Message Echoed is: " + result.getPayload().toString());
    }
    
    public void testUnsecuredWS1() throws Exception
    {
        unsecuredWS("vm://unsecured1", "unsecure1");
    }
    
    public void testUnsecuredWS2() throws Exception
    {
        unsecuredWS("vm://unsecured2", "unsecure2");
    }
    
    public void unsecuredWS(String endpoint, String message) throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(endpoint, message, null);
        assertNotNull(result.getPayload());
        assertTrue(result.getPayloadAsString().equalsIgnoreCase(message));
    }

    protected String getConfigResources()
    {
        return "axis-multiple-connectors.xml";
    }

}


