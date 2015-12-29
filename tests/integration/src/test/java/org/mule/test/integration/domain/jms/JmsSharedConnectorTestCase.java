/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.jms;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JmsSharedConnectorTestCase extends DomainFunctionalTestCase
{

    public static final String CLIENT_APP = "client";
    public static final String SERVER_APP = "server";
    public static final String CONNECTOR_PARAMETER = "connector=sharedJmsConnector";

    @Rule
    public SystemProperty transportScheme = new SystemProperty("scheme", getTransportScheme());
    @Rule
    public SystemProperty connectorParameter = new SystemProperty("connectorParameter", CONNECTOR_PARAMETER);

    private final String domainConfig;

    public JmsSharedConnectorTestCase(String domainConfig)
    {
        this.domainConfig = domainConfig;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"domain/jms/jms-activemq-embedded-shared-connector.xml"},
                {"domain/jms/jms-custom-shared-connector.xml"},
                {"domain/jms/jms-shared-connnector.xml"},
                {"domain/jms/jms-caching-connection-factory-shared-connnector.xml"}
        });
    }

    @Override
    protected String getDomainConfig()
    {
        return domainConfig;
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                new ApplicationConfig(CLIENT_APP, new String[] {"domain/jms/jms-client-app.xml"}),
                new ApplicationConfig(SERVER_APP, new String[] {"domain/jms/jms-server-app.xml"})
            };
    }

    @Test
    public void clientCallServerUsingRequestResponse() throws Exception
    {
        executeScenario("in", "out2");
    }

    @Test
    public void clientCallServerUsingRequestReply() throws Exception
    {
        executeScenario("in2", "out5");
    }

    private void executeScenario(final String inQueue,final String outQueue) throws Exception
    {
        getMuleContextForApp(CLIENT_APP).getClient().dispatch(queueAddress(inQueue), "test", null);
        final AtomicReference<MuleMessage> response = new AtomicReference<MuleMessage>();
        new PollingProber(10000,100).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                MuleMessage responseMessage;
                try
                {
                    responseMessage = getMuleContextForApp(CLIENT_APP).getClient().request(queueAddress(outQueue), 10);
                }
                catch (MuleException e)
                {
                    throw new RuntimeException(e);
                }
                if (responseMessage != null)
                {
                    response.set(responseMessage);
                    return true;
                }
                return false;
            }

            @Override
            public String describeFailure()
            {
                return "response message never arrived";
            }
        });
        assertThat(getPayloadAsString(response.get(), getMuleContextForApp(CLIENT_APP)), is("works"));
    }

    private String queueAddress(String inQueue)
    {
        return String.format("%s://%s?%s", getTransportScheme(), inQueue, CONNECTOR_PARAMETER);
    }

    protected String getTransportScheme()
    {
        return "jms";
    }
}
