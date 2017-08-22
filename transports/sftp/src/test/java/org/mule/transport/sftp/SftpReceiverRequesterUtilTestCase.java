/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.sftp.notification.SftpNotifier;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SftpReceiverRequesterUtilTestCase extends AbstractMuleContextTestCase
{

    private static final String FILE_TXT = "file.txt";

    @Rule
    public ExpectedException expectedException = none();

    @Mock
    private ImmutableEndpoint endpoint;

    @Mock
    private SftpConnector connector;

    @Mock
    private SftpNotifier notifier;

    @Mock
    private SftpClient sftpClient;

    @Mock
    private SftpUtil sftpUtil;


    @Before
    public void setUp() throws Exception
    {
        when(endpoint.getConnector()).thenReturn(connector);
        when(connector.createSftpClient(endpoint)).thenReturn(sftpClient);
        when(connector.createSftpClient(endpoint, notifier)).thenReturn(sftpClient);
        when(sftpClient.retrieveFile(FILE_TXT)).thenThrow(IOException.class);
        when(sftpUtil.getSizeCheckWaitTime()).thenReturn(1L);
    }

    @Test
    public void clientIsReleasedWhenRetrievingFails() throws Exception
    {
        SftpReceiverRequesterUtil requesterUtil = new SftpReceiverRequesterUtil(endpoint);

        expectedException.expect(IOException.class);
        try
        {
            requesterUtil.retrieveFile(FILE_TXT, notifier);
        }
        finally
        {
            verify(connector).releaseClient(endpoint, sftpClient);
        }
    }

    /**
     * Verifies an stable file (i.e. a file whose size doesn't change) is returned when fileAge is disabled.
     */
    @Test
    public void testReturnsStableFile() throws Exception
    {
        final String stableFile = "file1";
        when(connector.getFileAge()).thenReturn(-1L);
        when(sftpClient.listFiles()).thenReturn(new String[] {stableFile});
        when(sftpClient.getSize(stableFile)).thenReturn(1024L);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(1));
        assertThat(completedFiles[0], is(stableFile));
    }

    /**
     * Verifies an unstable file (i.e. a file whose size doesn't change) is not returned when fileAge is disabled.
     */
    @Test
    public void testNotReturnUnstableFile() throws Exception
    {
        final String unstableFile = "file1";
        when(connector.getFileAge()).thenReturn(-1L);
        when(sftpClient.listFiles()).thenReturn(new String[] {unstableFile});
        when(sftpClient.getSize(unstableFile)).thenReturn(1024L, 2048L);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(0));
    }

    /**
     * Verifies an aged file is returned when checkSize is disabled.
     */
    @Test
    public void testReturnsAgedFile() throws Exception
    {
        final String agedFile = "file1";
        when(connector.getCheckFileAge()).thenReturn(true);
        when(connector.getFileAge()).thenReturn(20L);
        when(sftpUtil.getSizeCheckWaitTime()).thenReturn(-1L);
        when(sftpClient.listFiles()).thenReturn(new String[] {agedFile});
        when(sftpClient.getLastModifiedTime(agedFile)).thenReturn(1L);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(1));
        assertThat(completedFiles[0], is(agedFile));
    }

    /**
     * Verifies an not aged file is not returned when checkSize is disabled.
     */
    @Test
    public void testNotReturnNotAgedFile() throws Exception
    {
        final String notAgedFile = "file1";
        when(connector.getCheckFileAge()).thenReturn(true);
        when(connector.getFileAge()).thenReturn(20L);
        when(sftpUtil.getSizeCheckWaitTime()).thenReturn(-1L);
        when(sftpClient.listFiles()).thenReturn(new String[] {notAgedFile});
        when(sftpClient.getLastModifiedTime(notAgedFile)).thenReturn(System.currentTimeMillis() * 2);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(0));
    }

    /**
     * Verifies an aged and stable file is returned when fileAge and checkSize are enabled.
     */
    @Test
    public void testReturnsAgedAndStableFile() throws Exception
    {
        final String stableAndAgedFile = "file1";
        when(connector.getCheckFileAge()).thenReturn(true);
        when(connector.getFileAge()).thenReturn(20L);
        when(sftpClient.listFiles()).thenReturn(new String[] {stableAndAgedFile});
        when(sftpClient.getSize(stableAndAgedFile)).thenReturn(1024L);
        when(sftpClient.getLastModifiedTime(stableAndAgedFile)).thenReturn(1L);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(1));
        assertThat(completedFiles[0], is(stableAndAgedFile));
    }

    /**
     * Verifies an aged but unstable file is not returned when fileAge and checkSize are enabled.
     */
    @Test
    public void testNotReturnAgedButUnstableFile() throws Exception
    {
        final String agedButUnstableFile = "file1";
        when(connector.getCheckFileAge()).thenReturn(true);
        when(connector.getFileAge()).thenReturn(20L);
        when(sftpClient.listFiles()).thenReturn(new String[] {agedButUnstableFile});
        when(sftpClient.getSize(agedButUnstableFile)).thenReturn(1024L, 2048L);
        when(sftpClient.getLastModifiedTime(agedButUnstableFile)).thenReturn(1L);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(0));
    }

    /**
     * Verifies an stable but not aged file is returned when fileAge and checkSize are enabled.
     */
    @Test
    public void testNotReturnStableButNotAgedFile() throws Exception
    {
        final String stableButNotAgeFile = "file1";
        when(connector.getCheckFileAge()).thenReturn(true);
        when(connector.getFileAge()).thenReturn(20L);
        when(sftpClient.listFiles()).thenReturn(new String[] {stableButNotAgeFile});
        when(sftpClient.getSize(stableButNotAgeFile)).thenReturn(1024L);
        when(sftpClient.getLastModifiedTime(stableButNotAgeFile)).thenReturn(System.currentTimeMillis() * 2);
        SftpReceiverRequesterUtil requesterUtil = new TestSftpReceiverRequesterUtil(endpoint);
        String[] completedFiles = requesterUtil.getAvailableFiles(false);
        assertThat(completedFiles.length, is(0));
    }

    private class TestSftpReceiverRequesterUtil extends SftpReceiverRequesterUtil
    {

        public TestSftpReceiverRequesterUtil(ImmutableEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected SftpUtil createSftpUtil(ImmutableEndpoint endpoint)
        {
            return sftpUtil;
        }

    }
}
