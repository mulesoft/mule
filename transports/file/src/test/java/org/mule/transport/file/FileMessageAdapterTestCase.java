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

import org.mule.DefaultMuleMessage;
import org.mule.RegistryContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class FileMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    private File message;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // The working directory is deleted on tearDown
        File dir = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory(), "tmp");
        if (!dir.exists())
        {
            dir.mkdirs();
        }

        message = File.createTempFile("simple", ".mule", dir);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage()
    {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new FileMessageAdapter(payload);
    }

    public void testMessageRetrieval2() throws Exception
    {
        Object message = new ReceiverFileInputStream((File) getValidMessage(), false, null);

        MessageAdapter adapter = createAdapter(message);
        MuleMessage muleMessage = new DefaultMuleMessage(adapter);

        doTestMessageEqualsPayload(message, adapter.getPayload());

        byte[] bytes = muleMessage.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = muleMessage.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());
    }

    protected void doTestMessageEqualsPayload(Object message, Object payload) throws Exception
    {

        // FileMessageAdaptor can be created from either an ReceiverFileInputStream
        // or File, so need to compare bytes

        byte[] messageBytes = null;
        byte[] payloadBytes = null;

        if (message instanceof File)
        {
            File file = (File) message;
            FileInputStream payloadFis = (FileInputStream) payload;
            messageBytes = new byte[(int) file.length()];
            payloadBytes = new byte[payloadFis.available()];
            new FileInputStream((File) message).read(messageBytes);
            ((FileInputStream) payload).read(payloadBytes);
        }
        else if (message instanceof FileInputStream)
        {
            FileInputStream fis = (FileInputStream) message;
            FileInputStream payloadFis = (FileInputStream) payload;
            messageBytes = new byte[fis.available()];
            payloadBytes = new byte[payloadFis.available()];
            fis.read(messageBytes);
            payloadFis.read(payloadBytes);
        }
        else
        {
            fail("FileMessageAdaptor supports File or FileInputStream");
        }
        assertTrue(Arrays.equals(messageBytes, payloadBytes));

    }
}
