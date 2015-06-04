/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transformer.types.MimeTypes;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FtpMuleMessageFactoryTestCase extends AbstractMuleTestCase
{

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void setsMimeType() throws Exception
    {
        FtpMuleMessageFactory factory = new FtpMuleMessageFactory();
        factory.setFtpClient(new TestFTPClient());

        MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);

        final FTPFile file = new FTPFile();
        file.setName("test.txt");

        MuleMessage message = factory.create(file, null, muleContext);

        assertThat(message.getDataType().getMimeType(), equalTo(MimeTypes.TEXT));
    }

    public static class TestFTPClient extends FTPClient
    {

        @Override
        public boolean retrieveFile(String remote, OutputStream local) throws IOException
        {
            return true;
        }
    }
}