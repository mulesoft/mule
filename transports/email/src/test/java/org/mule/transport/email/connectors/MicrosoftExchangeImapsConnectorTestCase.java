/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.connectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.api.transport.Connector;
import org.mule.transport.email.MicrosoftExchangeImapsConnector;
import org.mule.transport.email.SessionDetails;

import com.icegreen.greenmail.util.ServerSetup;

import org.junit.Test;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class MicrosoftExchangeImapsConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{
    public MicrosoftExchangeImapsConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_IMAPS);
    }

    @Override
    public Connector createConnector() throws Exception
    {
        MicrosoftExchangeImapsConnector connector = new MicrosoftExchangeImapsConnector(muleContext);
        connector.setName("ImapsConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());

        connector.setTrustStore("greenmail.jks");
        connector.setTrustStorePassword("changeit");
        return connector;
    }

    @Test
    public void testExtraJavaMailProperties() throws Exception
    {
        MicrosoftExchangeImapsConnector connector = (MicrosoftExchangeImapsConnector) getConnectorAndAssert();
        SessionDetails sessionDetails = connector.getSessionDetails(getTestInboundEndpoint("something"));

        assertThat("true", equalTo(sessionDetails.getSession().getProperty(String.format("mail.%s.auth.plain.disable",
                                                                                         ServerSetup.PROTOCOL_IMAPS))));
        assertThat("true", equalTo(sessionDetails.getSession().getProperty(String.format("mail.%s.auth.ntlm.disable",
                                                                                         ServerSetup.PROTOCOL_IMAPS))));
        assertThat("true", equalTo(sessionDetails.getSession().getProperty(String.format("mail.%s.auth.gssapi.disable",
                                                                                         ServerSetup.PROTOCOL_IMAPS))));

    }

}
