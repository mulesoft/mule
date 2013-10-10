/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * <code>FileContentsMuleMessageFactory</code> converts the
 * {@link InputStream}'s content into a <code>byte[]</code> as payload
 * for the {@link MuleMessage}.
 */
public class FileContentsMuleMessageFactory extends FileMuleMessageFactory
{
    public FileContentsMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{InputStream.class, File.class};
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        InputStream inputStream = convertToInputStream(transportMessage);
        byte[] payload = IOUtils.toByteArray(inputStream);
        inputStream.close();
        return payload;
    }

    private InputStream convertToInputStream(Object transportMessage) throws Exception
    {
        InputStream stream = null;

        if (transportMessage instanceof InputStream)
        {
            stream = (InputStream) transportMessage;
        }
        else if (transportMessage instanceof File)
        {
            stream = new FileInputStream((File) transportMessage);
        }

        return stream;
    }
}
