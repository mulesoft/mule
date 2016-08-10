/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.junit.Test;

public class OutboundEndpointPropertyMessageProcessorTestCase extends AbstractMessageProcessorTestCase {

  @Test
  public void testProcess() throws Exception {
    OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
    MessageProcessor mp = new OutboundEndpointPropertyMessageProcessor(endpoint);

    MuleEvent event = mp.process(createTestOutboundEvent());

    assertEquals(endpoint.getEndpointURI().getUri().toString(),
                 event.getMessage().getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY));
    assertSame(event, getCurrentEvent());
  }

}
