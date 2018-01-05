/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.AbstractMessageReceiverTestCase;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.transport.file.FileMessageReceiver.NOT_PROCESS_EMPTY_FILES_PROPERTY;

import java.util.Collection;

@RunWith(Parameterized.class)
public class ProcessEmptyFileTestCase extends AbstractMessageReceiverTestCase
{

    @Rule
    public SystemProperty processFileSystemProperty;

    @Rule
    public TemporaryFolder read = new TemporaryFolder();

    @Rule
    public TemporaryFolder move = new TemporaryFolder();

    private final MessageProcessor nextProcessor = mock(MessageProcessor.class);

    private final int numberOfInvocationsOfNextProcessor;

    public ProcessEmptyFileTestCase(String shouldProcessEmptyFiles)
    {
        this.processFileSystemProperty = new SystemProperty(NOT_PROCESS_EMPTY_FILES_PROPERTY, shouldProcessEmptyFiles);
        this.numberOfInvocationsOfNextProcessor = "false".equals(shouldProcessEmptyFiles) ? 1 : 0;
    }

    @Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {"true", "false"});
    }

    @Test
    public void processEmptyFile() throws Exception
    {
        FileMessageReceiver fmr = (FileMessageReceiver) getMessageReceiver();
        read.newFile("empty.tmp");
        fmr.initialise();
        fmr.doInitialise();
        fmr.setListener(nextProcessor);
        fmr.doConnect();
        fmr.poll();
        verify(nextProcessor, times(numberOfInvocationsOfNextProcessor)).process(any(MuleEvent.class));
    }

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Connector connector = endpoint.getConnector();
        connector.start();

        Service mockComponent = mock(Service.class);

        return new FileMessageReceiver(connector, mockComponent, endpoint,
                                       read.getRoot().getAbsolutePath(), move.getRoot().getAbsolutePath(), null, 1000);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("file://./simple");
    }

}
