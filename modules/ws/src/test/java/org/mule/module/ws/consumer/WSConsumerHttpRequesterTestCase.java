/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import org.junit.Test;;

/**
 * This tests "mocks" a proxy server through which a wsdl file is served.
 * The initialization of the ws consumer involved in the test app should
 * be routed through the http requester. If an URLConnection is used
 * instead of the http requester, the proxy will not be used. In this
 * way, we can verify that the http requester is being used.
 *
 */
public class WSConsumerHttpRequesterTestCase extends AbstractWSDLServerTestCase
{
	
	private static final int EXPECTED_NUMBER_OF_WS_CONSUMERS = 1;

    @Override
    protected String getConfigFile()
    {
        return "ws-consumer-http-requester-test-case.xml";
    }

    @Test
    public void consumerPresentInRegistry() throws Exception
    {
    	Map<String, WSConsumer> consumers = muleContext.getRegistry().lookupByType(WSConsumer.class);
		// if one consumer is present in the registry, the config
		// was correctly initialized
		assertThat(EXPECTED_NUMBER_OF_WS_CONSUMERS, equalTo(consumers.values().size()));
    }

}
