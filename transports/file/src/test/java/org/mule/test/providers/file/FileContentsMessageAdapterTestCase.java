/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.file;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.mule.MuleManager;
import org.mule.providers.file.FileContentsMessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

public class FileContentsMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private String validMessageContent = "Yabbadabbadooo!";
    private byte[] validMessage = validMessageContent.getBytes();
    private File messageFile;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // The working directory is deleted on tearDown
        File dir = new File(MuleManager.getConfiguration().getWorkingDirectory(), "tmp");
        if (!dir.exists())
        {
            dir.mkdirs();
        }

        messageFile = File.createTempFile("simple", ".mule", dir);
        FileUtils.writeStringToFile(messageFile, validMessageContent, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage()
    {
        return validMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        if (payload.equals(validMessage))
        {
            return new FileContentsMessageAdapter(messageFile);
        }
        else
        {
            // properly throw
            return new FileContentsMessageAdapter(payload);
        }
    }

    // overridden to properly check the byte[] by content and not just by reference
    public void doTestMessageEqualsPayload(Object message, Object payload) throws Exception
    {
        if (message instanceof byte[] && payload instanceof byte[])
        {
            assertTrue(Arrays.equals((byte[])message, (byte[])payload));
        }
        else
        {
            fail("message and payload must both be byte[]");
        }
    }

    public void testMessageContentsProperlyLoaded() throws Exception
    {
        // get new message adapter to test
        UMOMessageAdapter adapter = new FileContentsMessageAdapter(messageFile);

        // delete the file before accessing the payload
        assertTrue(messageFile.delete());

        // slight detour for testing :)
        doTestMessageEqualsPayload(validMessage, adapter.getPayload());
    }

    public void testMultipleSetMessageCalls() throws Exception
    {
        // get new message adapter to test
        AccessibleFCMAdapter adapter = new AccessibleFCMAdapter(messageFile);

        // access first payload
        doTestMessageEqualsPayload(validMessage, adapter.getPayload());

        // create another source file
        String secondMessageContent = "Hooray";
        byte[] secondMessage = secondMessageContent.getBytes();
        File secondFile = File.createTempFile("simple2", ".mule", messageFile.getParentFile());
        FileUtils.writeStringToFile(secondFile, secondMessageContent, null);

        // replace the first message content
        adapter.setMessage(secondFile);

        // make sure the file was properly read
        doTestMessageEqualsPayload(secondMessage, adapter.getPayload());
    }

}

// need this since setMessage is protected and we're in a different package
class AccessibleFCMAdapter extends FileContentsMessageAdapter
{
    private static final long serialVersionUID = -985644721173808557L;

    public AccessibleFCMAdapter(Object message) throws MessagingException
    {
        super(message);
    }

    protected void setMessage(File message) throws MessagingException
    {
        super.setMessage(message);
    }

}
