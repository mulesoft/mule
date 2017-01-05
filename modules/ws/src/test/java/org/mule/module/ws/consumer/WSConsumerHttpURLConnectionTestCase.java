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
 * This tests "mocks" a server through which a wsdl file is served. The
 * initialization of the ws consumer involved in the test app should be routed
 * through an url connection. If an http requester is used instead of the url
 * connection, the proxy will be used. In this way, we can verify that the url
 * is being used.
 *
 */
public class WSConsumerHttpURLConnectionTestCase extends AbstractWSDLServerTestCase {

	private static final int EXPECTED_NUMBER_OF_WS_CONSUMERS = 2;
	
	@Override
	protected String getConfigFile() {
		return "ws-consumer-url-connection-test-case.xml";
	}

	@Test
	public void twoConsumerPresentInRegistry() throws Exception {
		Map<String, WSConsumer> consumers = muleContext.getRegistry().lookupByType(WSConsumer.class);
		// if the two consumers are present in the registry, the config
		// was correctly initialized
		assertThat(EXPECTED_NUMBER_OF_WS_CONSUMERS, equalTo(consumers.values().size()));
	}

}
