/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * JUnit test for SftpClient
 *
 * @author Lennart Häggkvist
 */
@SmallTest
public class SftpClientTestCase extends AbstractMuleTestCase
{

    private final String fileName = "fileName";
    private final String destDir = "destDir";

    @Test
    public void testGetAbsolutePath()
    {
        SftpClient client = new SftpClient("hostName");
        client.setHome("/home/user");

        // Assuming address="sftp://user@host/PATH" and thus the path always start
        // with "/"
        assertEquals("hostName", client.getHost());

        // Relative paths
        assertEquals("/home/user/foo", client.getAbsolutePath("/~/foo"));
        assertEquals("/home/user/foo/bar", client.getAbsolutePath("/~/foo/bar"));

        // Two calls to getAbsolutePath should return the same
        assertEquals("/home/user/foo/bar", client.getAbsolutePath(client.getAbsolutePath("/~/foo/bar")));

        // Absolute path
        assertEquals("/opt/mule/files", client.getAbsolutePath("/opt/mule/files"));

        // If the path did not contain any '/' we should not assume it is an relative
        // path
        assertEquals("foo", client.getAbsolutePath("foo"));
    }

    @Test(expected = IOException.class)
    public void duplicateHandlingThrowException() throws Exception
    {
        getSftpClientSpy().duplicateHandling(destDir, fileName, SftpConnector.PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION);
    }

    @Test
    public void duplicateHandlingUniqueName() throws Exception
    {
        String newName = getSftpClientSpy().duplicateHandling(destDir, fileName, SftpConnector.PROPERTY_DUPLICATE_HANDLING_ASS_SEQ_NO);
        assertFalse(fileName.equals(newName));
    }

    @Test
    public void duplicateHandlingOverwrite() throws Exception
    {
        String newName = getSftpClientSpy().duplicateHandling(destDir, fileName, SftpConnector.PROPERTY_DUPLICATE_HANDLING_OVERWRITE);
        assertEquals(fileName, newName);
    }

    @Test
    public void causedByShouldBeAnSftpException() throws Exception
    {
        // setup mock channel to always throw an SftpException
        ChannelSftp mockChannel = mock(ChannelSftp.class);
        when(mockChannel.ls(anyString())).thenThrow(new SftpException(1, destDir));
        try
        {
            SftpClient client = new SftpClient("local");
            Field channelField = client.getClass().getDeclaredField("channelSftp");
            channelField.setAccessible(true);

            // set the mockChannel from above on the client
            channelField.set(client, mockChannel);

            /*
             * should throw an exception, but we need to make sure the
             * 'caused by...' portion of the exception is correctly set
             */
            client.listFiles(destDir);
        } catch (IOException e)
        {
            if (e.getCause() == null)
            {
                // cause was not correct, so the test fails
                fail("no cause provided, should have been SftpException");
            }
            final Class actualClass = e.getCause().getClass();
            assertEquals(actualClass, SftpException.class);
        } catch (Exception e)
        {
            // wrong exception thrown, so the test fails
            fail("cause should have been SftpException");
        }
    }

    private SftpClient getSftpClientSpy() throws IOException
    {
        SftpClient sftp = new SftpClient("local");
        SftpClient spy = spy(sftp);
        doReturn(new String[]{fileName}).when(spy).listFiles(destDir);
        return spy;
    }
}
