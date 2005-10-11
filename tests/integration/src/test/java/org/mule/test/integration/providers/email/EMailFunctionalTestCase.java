/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.email;

import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

public class EMailFunctionalTestCase extends AbstractMuleTestCase
{
    public void testPopRoundtrip() throws Exception
    {
        doRoundtrip("pop3://muletestbox:testbox1@pop.mail.yahoo.co.uk",
                "smtp://muletestbox:testbox1@smtp.mail.yahoo.co.uk?address=muletestbox@yahoo.co.uk");
    }

    public void testSecurePopRoundtrip() throws Exception
    {
        doRoundtrip("pop3s://muletestbox:testbox@pop.gmail.com",
                "smtps://muletestbox:testbox@smtp.gmail.com?address=muletestbox@gmail.com");
    }

    public void testSecureImapRoundtrip() throws Exception
    {
        //todo When Gmail support it
        //doRoundtrip("imaps://muletestbox:testbox@pop.gmail.com",
        //        "smtps://muletestbox:testbox@smtp.gmail.com?address=muletestbox@gmail.com");
    }

    public void doRoundtrip(String receiveUrl, String sendUrl) throws Exception
    {

        MuleClient mc = new MuleClient();
        UMOMessage msg = mc.receive(receiveUrl, 5000);
        while(msg!=null) {
            msg = mc.receive(receiveUrl, 5000);
            System.out.println("Received:" + msg.getPayloadAsString());
        }

        String messageString = "testtesttesttesttesttest";
        Map props = new HashMap();
        
        mc.sendNoReceive(sendUrl, messageString, props);
        msg = mc.receive(receiveUrl, 20000);
        assertNotNull(msg);
        assertTrue(msg.getPayloadAsString().indexOf(messageString) > -1);
    }

}
