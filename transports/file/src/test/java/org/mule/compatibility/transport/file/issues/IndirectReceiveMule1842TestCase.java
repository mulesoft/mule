/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file.issues;

import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.file.AbstractFileFunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.construct.Flow;

import java.io.File;

import org.junit.Test;

/**
 * This used to be part of FileFunctionalTest; moved here to allow isolation of individual case.
 */
public class IndirectReceiveMule1842TestCase extends AbstractFileFunctionalTestCase {

  @Test
  public void testIndirectRequest() throws Exception {
    File target = initForRequest();

    // add a receiver endpoint that will poll the readFromDirectory
    Object relay = muleContext.getRegistry().lookupObject("relay");
    assertNotNull(relay);
    String url = fileToUrl(target) + "?connector=receiveConnector";
    logger.debug(url);

    InboundEndpoint endpoint = getEndpointFactory().getInboundEndpoint(url);

    ((CompositeMessageSource) ((Flow) relay).getMessageSource()).addSource(endpoint);

    ((Stoppable) relay).stop();
    ((Startable) relay).start();

    // then read from the queue that the polling receiver will write to
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.request("receive", 3000).getRight().get();
    checkReceivedMessage(message);
  }
}
