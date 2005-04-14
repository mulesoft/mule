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
package org.mule.test.providers.email;

import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Pop3FunctionalTestCase extends AbstractMuleTestCase
{
    public void testMailRoundTrip() throws Exception
    {

//        SmtpConnector smtp = new SmtpConnector();
//        smtp.setHostname("mail.muleumo.org");
//        smtp.setDoThreading(false);
//        smtp.setFromAddress("ross@muleumo.org");
//        smtp.initialise();
//        smtp.start();
//        UMOEvent event = getTestEvent("Mule Mail Testing");
//        event.getEndpointName().setEndpointURI("mule@muleumo.org");
//
//        smtp.getDispatcher().dispatch(event);
//
//        Pop3Connector pop3 = new Pop3Connector();
//        UMOMessage message = pop3.getDispatcher().receive("pop3://mule:???@mail.muleumo.org", 4000);
//        assertNotNull(message);
//        assertNotNull(message.getPayload());
//        assertEquals("[No Subject]", message.getProperty(MailMessageAdapter.PROPERTY_SUBJECT));
//        assertEquals("ross@muleumo.org", message.getProperty(MailMessageAdapter.PROPERTY_FROM_ADDRESS));

    }
}
