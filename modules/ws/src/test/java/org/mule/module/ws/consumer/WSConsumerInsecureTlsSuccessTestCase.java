/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_USE_CONNECTOR_TO_RETRIEVE_WSDL;

import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

/**
 * This tests "mocks" an HTTPS server through which a wsdl file is served. The initialization of the ws consumer involved
 * in the test app should be routed through the http requester configured with an insecure TLS context.
 * If an URLConnection is used instead of the http requester, the TLS context will not be used.
 */
public class WSConsumerInsecureTlsSuccessTestCase extends AbstractWSDLServerTlsTestCase
{
    @Rule
    public SystemProperty useConnectorToRetrieveWsdl = new SystemProperty(MULE_USE_CONNECTOR_TO_RETRIEVE_WSDL, "true");

    @Override
    protected String getConfigFile()
    {
        return "ws-consumer-wsdl-insecure-tls-config.xml";
    }

    @Test
    public void consumerPresentInRegistry() throws Exception
    {
        Map<String, WSConsumer> consumers = muleContext.getRegistry().lookupByType(WSConsumer.class);
        // if one consumer is present in the registry, the config was correctly initialized
        assertThat(consumers.values().size(), equalTo(1));
    }

}
