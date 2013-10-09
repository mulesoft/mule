/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.file.FileConnector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpMuleMessageFactory extends AbstractMuleMessageFactory
{

    private FTPClient ftpClient;
    private boolean streaming;

    public FtpMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        FTPFile file = (FTPFile) transportMessage;
        if (streaming)
        {
            InputStream stream = ftpClient.retrieveFileStream(file.getName());
            if (stream == null)
            {
                throw new IOException(MessageFormat.format("Failed to retrieve file {0}. Ftp error: {1}",
                    file.getName(), ftpClient.getReplyCode()));
            }
            return stream;
        }
        else
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ftpClient.retrieveFile(file.getName(), baos))
            {
                throw new IOException(MessageFormat.format("Failed to retrieve file {0}. Ftp error: {1}",
                    file.getName(), ftpClient.getReplyCode()));
            }
            byte[] bytes = baos.toByteArray();
            if (bytes.length > 0)
            {
                return bytes;
            }
            else
            {
                throw new IOException("File " + file.getName() + " is empty (zero bytes)");
            }
        }
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{FTPFile.class};
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        super.addProperties(message, transportMessage);
        
        FTPFile file = (FTPFile) transportMessage;
        message.setOutboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, file.getName());
        message.setOutboundProperty(FileConnector.PROPERTY_FILE_SIZE, file.getSize());
        message.setOutboundProperty(FileConnector.PROPERTY_FILE_TIMESTAMP, file.getTimestamp());
    }

    public void setFtpClient(FTPClient ftpClient)
    {
        this.ftpClient = ftpClient;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }

}
