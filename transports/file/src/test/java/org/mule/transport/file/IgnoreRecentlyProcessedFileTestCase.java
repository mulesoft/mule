/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.transport.file.FileMessageReceiver.MULE_NOT_PROCESSING_RECENTLY_FILES_PERIOD;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.AbstractMessageReceiverTestCase;

@RunWith(Parameterized.class)
public class IgnoreRecentlyProcessedFileTestCase extends AbstractMessageReceiverTestCase
{

    private static final String DUMMY_FILE = "dummy.tmp";

    @Rule
    public SystemProperty ignoreRecentlyProcessedFilePeriod;

    @Rule
    public TemporaryFolder read = new TemporaryFolder();

    private final MessageProcessor nextProcessor = mock(MessageProcessor.class);


    public IgnoreRecentlyProcessedFileTestCase(String notProcessingRecentlyFilesPeriod)
    {
        this.ignoreRecentlyProcessedFilePeriod = new SystemProperty(MULE_NOT_PROCESSING_RECENTLY_FILES_PERIOD, notProcessingRecentlyFilesPeriod);
    }

    @Parameters
    public static Collection<Object> data()
    {
        return asList(new Object[] {"-1", "10000"});
    }

    @Test
    public void processFiles() throws Exception
    {
        FileMessageReceiver fmr = (FileMessageReceiver) getMessageReceiver();
        read.newFile(DUMMY_FILE);
        fmr.initialise();
        fmr.doInitialise();
        fmr.setListener(nextProcessor);
        fmr.doConnect();
        fmr.poll();
        verify(nextProcessor, times(1)).process(any(MuleEvent.class));
        read.newFile(DUMMY_FILE);
        fmr.poll();
        verifyProcessedFiles(2, 1);
    }

    protected void verifyProcessedFiles(Integer expectedFilesWithoutRecentlyFilesIgnore, Integer expectedFilesWithRecentlyFilesIgnore) throws MuleException
    {
        verify(nextProcessor, times(getExpectedProcessedTimes(expectedFilesWithoutRecentlyFilesIgnore, expectedFilesWithRecentlyFilesIgnore))).process(
                any(MuleEvent.class));

    }

    protected Integer getExpectedProcessedTimes(Integer expectedFilesWithoutRecentlyFilesIgnore, Integer expectedFilesWithRecentlyFilesIgnore)
    {
        return Integer.parseInt(ignoreRecentlyProcessedFilePeriod.getValue()) == -1 ? expectedFilesWithoutRecentlyFilesIgnore : expectedFilesWithRecentlyFilesIgnore;
    }

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Connector connector = endpoint.getConnector();
        ((FileConnector) connector).setStreaming(false);
        connector.start();

        Service mockComponent = mock(Service.class);

        return new FileMessageReceiver(connector, mockComponent, endpoint,
                read.getRoot().getAbsolutePath(), null, null, 1000);
    }

    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        return muleContext.getEndpointFactory().getInboundEndpoint("file://./simple");
    }

}
