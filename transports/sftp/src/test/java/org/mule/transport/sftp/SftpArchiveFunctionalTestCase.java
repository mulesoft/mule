/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the archive features.
 */
public class SftpArchiveFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 15000;

    private static final String FILE1_TXT = "file1.txt";
    private static final String FILE2_TXT = "file2.txt";
    private static final String FILE3_TXT = "file3.txt";
    private static final String FILE4_TXT = "file4.txt";

    private static final String FILE2_TMP_REGEXP = "file2_.+";
    private static final String FILE3_TMP_REGEXP = "file3_.+";

    private static final String TMP_SENDING = "tmp_sending";
    private static final String TMP_RECEIVING = "tmp_receiving";

    private static final String INBOUND_ENDPOINT1 = "inboundEndpoint1";
    private static final String INBOUND_ENDPOINT2 = "inboundEndpoint2";
    private static final String INBOUND_ENDPOINT3 = "inboundEndpoint3";
    private static final String INBOUND_ENDPOINT4 = "inboundEndpoint4";

    // Size of the generated stream - 2 Mb
    final static int SEND_SIZE = 1024 * 1024 * 2;

    private String archive = null;
    private String archiveCanonicalPath = null;

    public SftpArchiveFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-sftp-archive-test-config-service.xml"},
            {ConfigVariant.FLOW, "mule-sftp-archive-test-config-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // this block moved from the constructor to allow this test to be skipped if
        // the resource isn't found via
        // AbstractMuleTestCase.isDisabledInThisEnvironment
        ResourceBundle rb = ResourceBundle.getBundle("sftp-settings");
        archive = rb.getString("ARCHIVE");
        archiveCanonicalPath = new File(archive).getCanonicalPath();

        recursiveDeleteInLocalFilesystem(new File(archive));
        initEndpointDirectories(new String[]{"receiving1", "receiving2", "receiving3", "receiving4"},
            new String[]{INBOUND_ENDPOINT1, INBOUND_ENDPOINT2, INBOUND_ENDPOINT3, INBOUND_ENDPOINT4});
    }

    /**
     * Test plain archive functionality with no extra features enabled
     */
    @Test
    public void testArchive1() throws Exception
    {
        executeBaseTest(INBOUND_ENDPOINT1, "vm://test.upload1", FILE1_TXT, SEND_SIZE, "receiving1", TIMEOUT);

        // Assert that the file now exists in the archive
        assertFilesInLocalFilesystem(archive, FILE1_TXT);

        // And that the file is gone from the inbound endpoint
        assertNoFilesInEndpoint(INBOUND_ENDPOINT1);
    }

    /**
     * Test archive functionality with full usage of temp-dir and creation of unique
     * names of temp-files
     */
    @Test
    public void testArchive2() throws Exception
    {
        executeBaseTest(INBOUND_ENDPOINT2, "vm://test.upload2", FILE2_TXT, SEND_SIZE, "receiving2", TIMEOUT);

        // Assert that the file now exists in the archive
        // (with some unknown timestamp in the filename)
        // and that the tmp-archive-folders are empty
        assertFilesInLocalFilesystem(archive, new String[]{TMP_RECEIVING, TMP_SENDING, FILE2_TMP_REGEXP});
        assertNoFilesInLocalFilesystem(archive + File.separatorChar + TMP_RECEIVING);
        assertNoFilesInLocalFilesystem(archive + File.separatorChar + TMP_SENDING);

        // Assert that the file is gone from the inbound endpoint (including its
        // tmp-folders)
        // Note that directories are not returned in this listing
        assertNoFilesInEndpoint(INBOUND_ENDPOINT2);
        assertNoFilesInEndpoint(INBOUND_ENDPOINT2, TMP_RECEIVING);
        assertNoFilesInEndpoint(INBOUND_ENDPOINT2, TMP_SENDING);
    }

    /**
     * Test archive functionality with usage of temp-dir for inbound and outbound
     * endpoints with creation of unique names of temp-files but not for the archive
     */
    @Test
    public void testArchive3() throws Exception
    {
        executeBaseTest(INBOUND_ENDPOINT3, "vm://test.upload3", FILE3_TXT, SEND_SIZE, "receiving3", TIMEOUT);

        // Assert that the file now exists in the archive
        // (with some unknown timestamp in the filename)
        assertFilesInLocalFilesystem(archive, FILE3_TMP_REGEXP);

        // Assert that the file is gone from the inbound endpoint (including its
        // tmp-folders)
        // Note that directories are not returned in this listing
        assertNoFilesInEndpoint(INBOUND_ENDPOINT3);
        assertNoFilesInEndpoint(INBOUND_ENDPOINT3, TMP_RECEIVING);
        assertNoFilesInEndpoint(INBOUND_ENDPOINT3, TMP_SENDING);
    }

    /**
     * Test archive functionality with usage of temp-dir for archive but not for
     * inbound and outbound endpoints
     */
    @Test
    public void testArchive4() throws Exception
    {
        executeBaseTest(INBOUND_ENDPOINT4, "vm://test.upload4", FILE4_TXT, SEND_SIZE, "receiving4", TIMEOUT);

        // Assert that the file now exists in the archive
        // and that the tmp-archive-folders are empty
        assertFilesInLocalFilesystem(archive, new String[]{TMP_RECEIVING, TMP_SENDING, FILE4_TXT});
        assertNoFilesInLocalFilesystem(archive + File.separatorChar + TMP_RECEIVING);
        assertNoFilesInLocalFilesystem(archive + File.separatorChar + TMP_SENDING);

        // Assert that the file is gone from the inbound endpoint
        assertNoFilesInEndpoint(INBOUND_ENDPOINT4);
    }

    /**
     * Test error handling with plain archive functionality with no extra features
     * enabled
     */
    @Test
    public void testCantWriteToArchive1() throws Exception
    {
        makeArchiveReadOnly();
        try
        {
            executeBaseTest(INBOUND_ENDPOINT1, "vm://test.upload1", FILE1_TXT, SEND_SIZE, "receiving1",
                TIMEOUT, "sftp");
            fail("Expected error");
        }
        catch (Exception e)
        {
            assertNotNull(e);
            assertTrue(e instanceof IOException);
            assertEquals("Destination folder is not writeable: " + archiveCanonicalPath, e.getMessage());
        }

        // Assert that file still exists in the inbound endpoint after the failure
        assertFilesInEndpoint(INBOUND_ENDPOINT1, FILE1_TXT);

        // Assert that no files exists in the archive after the error
        assertNoFilesInLocalFilesystem(archive);
    }

    /**
     * Test error handling with archive functionality with full usage of temp-dir and
     * creation of unique names of temp-files
     */
    @Test
    public void testCantWriteToArchive2() throws Exception
    {
        fail();
        makeArchiveTmpFolderReadOnly();
        makeArchiveReadOnly();
        try
        {
            executeBaseTest(INBOUND_ENDPOINT2, "vm://test.upload2", FILE2_TXT, SEND_SIZE, "receiving2",
                TIMEOUT, "sftp");
            fail("Expected error");
        }
        catch (Exception e)
        {
            assertNotNull(e);
            assertTrue(e instanceof IOException);
            assertEquals("Destination folder is not writeable: " + archiveCanonicalPath + File.separatorChar
                         + TMP_RECEIVING, e.getMessage());
        }

        // Assert that the file still exists in the inbound endpoint's tmp-folder
        // after the failure
        // (with some unknown timestamp in the filename)
        // Note that directories are not returned in this listing
        assertNoFilesInEndpoint(INBOUND_ENDPOINT2);
        assertNoFilesInEndpoint(INBOUND_ENDPOINT2, TMP_RECEIVING);
        assertFilesInEndpoint(INBOUND_ENDPOINT2, TMP_SENDING, FILE2_TMP_REGEXP);

        // Assert that no files exists in the archive after the error except from the
        // temp-folders
        assertFilesInLocalFilesystem(archive, new String[]{TMP_RECEIVING, TMP_SENDING});
    }

    /**
     * Test error handling with archive functionality with usage of temp-dir for
     * inbound and outbound endpoints with creation of unique names of temp-files but
     * not for the archive
     */
    @Test
    public void testCantWriteToArchive3() throws Exception
    {
        makeArchiveReadOnly();
        try
        {
            executeBaseTest(INBOUND_ENDPOINT3, "vm://test.upload3", FILE3_TXT, SEND_SIZE, "receiving3",
                TIMEOUT, "sftp");
            fail("Expected error");
        }
        catch (Exception e)
        {
            assertNotNull(e);
            assertTrue(e instanceof IOException);
            assertEquals("Destination folder is not writeable: " + archiveCanonicalPath, e.getMessage());
        }

        // Assert that the file still exists in the inbound endpoint's tmp-folder
        // after the failure
        // (with some unknown timestamp in the filename)
        // Note that directories are not returned in this listing
        assertNoFilesInEndpoint(INBOUND_ENDPOINT3);
        assertNoFilesInEndpoint(INBOUND_ENDPOINT3, TMP_RECEIVING);
        assertFilesInEndpoint(INBOUND_ENDPOINT3, TMP_SENDING, FILE3_TMP_REGEXP);

        // Assert that no files exists in the archive after the error
        assertNoFilesInLocalFilesystem(archive);
    }

    /**
     * Test error handling with archive functionality with usage of temp-dir for
     * archive but not for inbound and outbound endpoints
     */
    @Test
    public void testCantWriteToArchive4() throws Exception
    {
        makeArchiveTmpFolderReadOnly();
        makeArchiveReadOnly();
        try
        {
            executeBaseTest(INBOUND_ENDPOINT4, "vm://test.upload4", FILE4_TXT, SEND_SIZE, "receiving4",
                TIMEOUT, "sftp");
            fail("Expected error");
        }
        catch (Exception e)
        {
            assertNotNull(e);
            assertTrue(e instanceof IOException);
            assertEquals("Destination folder is not writeable: " + archiveCanonicalPath + File.separatorChar
                         + TMP_RECEIVING, e.getMessage());
        }

        // Assert that file still exists in the inbound endpoint after the failure
        assertFilesInEndpoint(INBOUND_ENDPOINT4, FILE4_TXT);

        // Assert that no files exists in the archive after the error except from the
        // temp-folders
        assertFilesInLocalFilesystem(archive, new String[]{TMP_RECEIVING, TMP_SENDING});
    }

    private void makeArchiveReadOnly() throws IOException
    {
        makeFolderReadOnly(archive);
    }

    private void makeArchiveTmpFolderReadOnly() throws IOException
    {
        makeFolderReadOnly(archive + File.separator + TMP_SENDING);
        makeFolderReadOnly(archive + File.separator + TMP_RECEIVING);
    }

    private void makeFolderReadOnly(String folderName) throws IOException
    {
        File folder = new File(folderName);
        if (!folder.exists())
        {
            if (!folder.mkdirs())
            {
                throw new IOException("Failed to create folder: " + folderName);
            }
        }
        if (!folder.setReadOnly())
        {
            throw new IOException("Failed to make folder readonly: " + folderName);
        }
    }
}
