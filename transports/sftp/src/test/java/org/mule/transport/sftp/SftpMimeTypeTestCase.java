/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.transformer.types.MimeTypes.TEXT;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

public class SftpMimeTypeTestCase extends AbstractSftpFunctionalTestCase
{

    public static final String FILE_NAME = "file.txt";
    private static Latch latch;

    @Override
    protected String getConfigFile()
    {
        return "sftp-mime-type-config.xml";
    }

    @Override
    protected void setUpTestData() throws IOException
    {
        latch = new Latch();
    }

    @Test
    public void setsMimeType() throws Exception
    {
        sftpClient.storeFile(FILE_NAME, new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
        latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
        assertThat(SftpMessageProcessor.mimeType, equalTo(TEXT));
    }

    public static class SftpMessageProcessor implements MessageProcessor
    {

        private static String mimeType = null;

        @Override
        public MuleEvent process(MuleEvent event)
        {
            mimeType = event.getMessage().getDataType().getMimeType();
            latch.release();
            return event;
        }
    }
}

