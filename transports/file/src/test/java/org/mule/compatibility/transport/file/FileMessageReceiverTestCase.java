/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.transport.AbstractMessageReceiverTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileMessageReceiverTestCase extends AbstractMessageReceiverTestCase {

  @Rule
  public TemporaryFolder read = new TemporaryFolder();

  @Rule
  public TemporaryFolder move = new TemporaryFolder();

  public void testReceiver() throws Exception {
    // FIX A bit hard testing receive from a unit simple as we need to reg
    // listener etc.
    // file endpoint functions tests for this
  }

  @Test
  public void testNotProcessingEmptyFile() throws Exception {
    FileMessageReceiver fmr = (FileMessageReceiver) getMessageReceiver();
    read.newFile("empty.tmp");
    fmr.initialise();
    fmr.doInitialise();
    fmr.setListener(new Processor() {

      @Override
      public Event process(Event event) throws MuleException {
        fail("Should not process empty file");
        return null;
      }
    });
    fmr.doConnect();
    fmr.poll();
  }

  @Override
  public MessageReceiver getMessageReceiver() throws Exception {
    Connector connector = endpoint.getConnector();
    connector.start();

    return new FileMessageReceiver(connector, mock(Flow.class), endpoint,
                                   read.getRoot().getAbsolutePath(), move.getRoot().getAbsolutePath(), null, 1000);
  }

  @Override
  public InboundEndpoint getEndpoint() throws Exception {
    return getEndpointFactory().getInboundEndpoint("file://./simple");
  }
}
