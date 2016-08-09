/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.mockito.Mockito.mock;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.transport.AbstractMessageReceiverTestCase;
import org.mule.compatibility.transport.file.FileMessageReceiver;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;

public class FileMessageReceiverTestCase extends AbstractMessageReceiverTestCase {

  File read = FileUtils.newFile("testcasedata/read");
  File move = FileUtils.newFile("testcasedata/move");

  public void testReceiver() throws Exception {
    // FIX A bit hard testing receive from a unit simple as we need to reg
    // listener etc.
    // file endpoint functions tests for this
  }

  @Override
  public MessageReceiver getMessageReceiver() throws Exception {
    Connector connector = endpoint.getConnector();
    connector.start();

    read.deleteOnExit();
    move.deleteOnExit();

    return new FileMessageReceiver(connector, mock(Flow.class), endpoint, read.getAbsolutePath(), move.getAbsolutePath(), null,
                                   1000);
  }

  @Override
  public InboundEndpoint getEndpoint() throws Exception {
    return getEndpointFactory().getInboundEndpoint("file://./simple");
  }
}
