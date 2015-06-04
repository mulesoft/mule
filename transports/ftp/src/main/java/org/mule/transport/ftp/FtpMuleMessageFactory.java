/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import org.mule.DefaultMuleMessage;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.file.FileConnector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpMuleMessageFactory extends AbstractMuleMessageFactory
{

    private FTPClient ftpClient;
    private boolean streaming;
    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

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
            try
            {

                if (!ftpClient.retrieveFile(file.getName(), baos))
                {
                    throw new IOException(String.format("Failed to retrieve file %s. Ftp error: %s",
                                                               file.getName(), ftpClient.getReplyCode()));
                }

                return baos.toByteArray();
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new IOException(String.format("Failed to retrieve file %s due to unexpected exception", file.getName()), e);
            }
            catch (OutOfMemoryError e)
            {
                throw new IOException(String.format("Failed to retrieve file %s because it's larger than the current memory heap. " +
                                                    "Consider enabling streaming on the FTP connector", file.getName()), e);
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
        message.setInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, file.getName());
        message.setInboundProperty(FileConnector.PROPERTY_FILE_SIZE, file.getSize());
        message.setInboundProperty(FileConnector.PROPERTY_FILE_TIMESTAMP, file.getTimestamp());
    }

    @Override
    protected String getMimeType(Object transportMessage)
    {
        if (transportMessage instanceof FTPFile)
        {
            FTPFile file = (FTPFile) transportMessage;

            return mimetypesFileTypeMap.getContentType(file.getName().toLowerCase());
        }
        else
        {
            return null;
        }
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
