/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.model.streaming.DelegatingInputStream;
import org.mule.module.client.MuleClient;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

public class FileRequestorMoveDeleteTestCase extends AbstractFileMoveDeleteTestCase
{

    public void testMoveAndDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, true, true, true, null);

        assertRequested(request(inFile), inFile, true);
        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, true, true, false, null);

        assertRequested(request(inFile), inFile, true);
        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnlyStreaming() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, true, false, true, null);

        assertRequested(request(inFile), inFile, true);
        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDeleteStreaming() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, true, false, false, null);

        assertRequested(request(inFile), inFile, true);
        assertFiles(inFile, moveToDir, false, false);
    }

    public void testMoveAndDelete() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, false, true, true, null);

        assertRequested(request(inFile), inFile, false);
        assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnly() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, false, true, false, null);

        assertRequested(request(inFile), inFile, false);
        assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnly() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, false, false, true, null);

        assertRequested(request(inFile), inFile, false);
        assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDelete() throws Exception
    {
        File inFile = initForRequest();
        File moveToDir = configureConnector(inFile, false, false, false, null);

        assertRequested(request(inFile), inFile, false);
        assertFiles(inFile, moveToDir, false, false);
    }

    public void testMoveAndDeleteFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRequested(request(inFile), inFile, false);
        // assertFiles(inFile, moveToDir, true, true);
    }

    public void testMoveOnlyFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, true, false, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRequested(request(inFile), inFile, false);
        // assertFiles(inFile, moveToDir, true, false);
    }

    public void testDeleteOnlyFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, true, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRequested(request(inFile), inFile, false);
        // assertFiles(inFile, moveToDir, false, true);
    }

    public void testNoMoveNoDeleteFilePayload() throws Exception
    {
        File inFile = initForRequest();

        File moveToDir = configureConnector(inFile, false, false, false, FileMessageAdapter.class);

        // TODO MULE-3198
        // assertRequested(request(inFile), inFile, false);

        assertFiles(inFile, moveToDir, false, false);
    }

    protected void assertRequested(MuleMessage message, File inFile, boolean streaming)
        throws IOException, MessagingException, InterruptedException
    {
        // Allow time for deletes/moves, so we can then assert to check files that
        // shouldn't havn't been moved havn't
        Thread.sleep(2000);

        assertNotNull(message);

        assertNotNull(message.getAdapter());
        assertEquals(inFile.getName(), message.getAdapter().getProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
        if (streaming)
        {
            // Adaptor gets wrapped by requester because FileMessageAdaptor does not
            // implement MutableMessageAdaptor
            assertEquals(DefaultMessageAdapter.class, message.getAdapter().getClass());
        }
        else
        {
            assertEquals(FileContentsMessageAdapter.class, message.getAdapter().getClass());
        }

        assertNotNull(message.getPayload());
        if (streaming)
        {
            // We can't check ReceiverFileInputStream is received because it is
            // wrapped in a DelegatingInputStream
            assertTrue(message.getPayload() instanceof DelegatingInputStream);
            InputStream fis = (InputStream) message.getPayload();
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            IOUtils.copy(fis, byteOut);

            // close() not called yet, nothing should have been deleted or moved yet
            assertFilesUntouched(inFile);

            fis.close();
            String result = new String(byteOut.toByteArray());
            assertEquals(TEST_MESSAGE, result);
        }
        else
        {
            assertTrue(message.getPayload() instanceof byte[]);
            assertEquals(TEST_MESSAGE, new String((byte[]) message.getPayload()));
        }
    }

    protected MuleMessage request(File file) throws MuleException, MalformedURLException
    {
        MuleClient muleClient = new MuleClient();
        return muleClient.request(fileToUrl(file) + "?connector=moveDeleteConnector", 2000);
    }
}
