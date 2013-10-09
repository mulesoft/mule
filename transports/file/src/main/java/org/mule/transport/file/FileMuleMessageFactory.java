/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractMuleMessageFactory;

import java.io.File;
import java.io.InputStream;

/**
 * <code>FileMuleMessageFactory</code> creates a new {@link MuleMessage} with a
 * {@link File} or {@link InputStream} payload. Users can obtain the filename and
 * directory in the properties using <code>FileConnector.PROPERTY_FILENAME</code> and
 * <code>FileConnector.PROPERTY_DIRECTORY</code>.
 */
public class FileMuleMessageFactory extends AbstractMuleMessageFactory
{
    public FileMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{File.class, ReceiverFileInputStream.class};
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        return transportMessage;
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        super.addProperties(message, transportMessage);
        File file = convertToFile(transportMessage);
        setPropertiesFromFile(message, file);
    }

    protected File convertToFile(Object transportMessage)
    {
        File file = null;

        if (transportMessage instanceof File)
        {
            file = (File) transportMessage;
        }
        else if (transportMessage instanceof ReceiverFileInputStream)
        {
            file = ((ReceiverFileInputStream) transportMessage).getCurrentFile();
        }

        return file;
    }

    protected void setPropertiesFromFile(MuleMessage message, File file)
    {
        message.setOutboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, file.getName());
        message.setOutboundProperty(FileConnector.PROPERTY_DIRECTORY, file.getParent());
        message.setOutboundProperty(FileConnector.PROPERTY_FILE_SIZE, file.length());
        message.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, file.getName(), PropertyScope.INBOUND);
        message.setProperty(FileConnector.PROPERTY_DIRECTORY, file.getParent(), PropertyScope.INBOUND);
        message.setProperty(FileConnector.PROPERTY_FILE_SIZE, file.length(), PropertyScope.INBOUND);
        message.setProperty(FileConnector.PROPERTY_FILE_TIMESTAMP, file.lastModified(), PropertyScope.INBOUND);
    }
}
