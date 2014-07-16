/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Test;

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

    private SftpClient getSftpClientSpy() throws IOException
    {
        SftpClient sftp = new SftpClient("local");
        SftpClient spy = spy(sftp);
        doReturn(new String[]{fileName}).when(spy).listFiles(destDir);
        return spy;
    }
}
