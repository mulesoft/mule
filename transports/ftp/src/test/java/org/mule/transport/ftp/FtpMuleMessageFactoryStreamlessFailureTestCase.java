/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class FtpMuleMessageFactoryStreamlessFailureTestCase extends AbstractMuleTestCase
{

    private static final String FILE_NAME = "myFile";
    private static final String ENCODING = "UTF-8";

    private FtpMuleMessageFactory factory;

    @Mock
    private FTPClient client;

    @Mock
    private FTPFile file;

    @Before
    public void before() throws Exception
    {
        factory = new FtpMuleMessageFactory();
        factory.setStreaming(false);
        factory.setFtpClient(client);
        when(file.getName()).thenReturn(FILE_NAME);
    }

    @Test
    public void outOfMemoryError() throws Exception
    {
        assertThrowsException(new OutOfMemoryError());
    }

    @Test
    public void unexpectedException() throws Exception
    {
        assertThrowsException(new RuntimeException());
    }

    @Test
    public void expectedException() throws Exception
    {
        when(client.retrieveFile(same(FILE_NAME), any(OutputStream.class))).thenReturn(false);
        assertThrowsException(null);
    }


    private void assertThrowsException(Throwable t) throws Exception
    {
        if (t != null)
        {
            when(client.retrieveFile(same(FILE_NAME), any(OutputStream.class))).thenThrow(t);
        }
        assertExceptionAndCause(t);
    }

    private void assertExceptionAndCause(Throwable t) throws Exception
    {
        try
        {
            factory.extractPayload(file, ENCODING);
            fail("was expecting failure");
        }
        catch (IOException e)
        {
            assertThat(e.getCause(), is(t));
        }
    }

}
