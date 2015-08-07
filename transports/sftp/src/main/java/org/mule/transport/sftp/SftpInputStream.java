/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.MuleRuntimeException;
import org.mule.api.endpoint.ImmutableEndpoint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>SftpInputStream</code> wraps an sftp InputStream.
 */

public class SftpInputStream extends BufferedInputStream implements SftpStream
{
    private final Log logger = LogFactory.getLog(getClass());

    private SftpClient client;
    private boolean autoDelete = true;
    private String fileName;
    private boolean postProcessOnClose = false;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    private boolean errorOccured = false;
    private ImmutableEndpoint endpoint;

    // Log every 10 000 000 bytes read at debug-level
    // Good if really large files are transferred and you tend to get nervous by not
    // seeing any progress in the logfile...
    private static final int LOG_BYTE_INTERVAL = 10000000;
    private long bytesRead = 0;
    private long nextLevelToLogBytesRead = LOG_BYTE_INTERVAL;

    /**
     * A special sftp InputStream. The constructor creates the InputStream by calling
     * <code>SftpClient.retrieveFile(fileName)</code>. The client passed in is
     * destroyed when the stream is closed.
     * 
     * @param client The SftpClient instance. Will be destroyed when stream closed.
     * @param is The stream that should be used
     * @param fileName name of the file to be retrieved
     * @param autoDelete whether the file specified by fileName should be deleted
     * @param endpoint the endpoint associated to a specific client (connector) pool.
     * @throws Exception if failing to retrieve internal input stream.
     */
    public SftpInputStream(SftpClient client,
                           InputStream is,
                           String fileName,
                           boolean autoDelete,
                           ImmutableEndpoint endpoint) throws Exception
    {
        super(is);

        this.client = client;
        this.fileName = fileName;
        this.autoDelete = autoDelete;
        this.endpoint = endpoint;
    }

    @Override
    public synchronized int read() throws IOException
    {
        logReadBytes(1);
        return super.read();
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException
    {
        logReadBytes(len);
        return super.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        logReadBytes(b.length);
        return super.read(b);
    }

    @Override
    public void close() throws IOException
    {
        if (closed.compareAndSet(false, true))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Closing the stream for the file " + fileName);
            }

            try
            {
                super.close();
            }
            catch (IOException e)
            {
                logger.error("Error occurred while closing file " + fileName, e);
                throw e;
            }

            if (postProcessOnClose)
            {
                try
                {
                    postProcess();
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
        }
    }

    @Override
    public boolean isClosed()
    {
        return closed.get();
    }

    @Override
    public void postProcess() throws Exception
    {
        if (!client.isConnected())
        {
            return;
        }

        try
        {
            close();
            if (autoDelete && !errorOccured)
            {
                client.deleteFile(fileName);
            }
        }
        finally
        {
            // We should release the connection from the pool even if some error
            // occurs here
            releaseConnection();
        }

    }

    void releaseConnection()
    {
        try
        {
            ((SftpConnector) endpoint.getConnector()).releaseClient(endpoint, client);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }

    public void setErrorOccurred()
    {
        if (logger.isDebugEnabled()) logger.debug("setErrorOccurred() called");
        this.errorOccured = true;
    }

    @Override
    public void performPostProcessingOnClose(boolean postProcessOnClose)
    {
        this.postProcessOnClose = postProcessOnClose;
    }

    @Override
    public String toString()
    {
        return "SftpInputStream{" + "fileName='" + fileName + '\'' + " from endpoint="
               + endpoint.getEndpointURI() + '}';
    }

    private void logReadBytes(int newBytesRead)
    {
        if (!logger.isDebugEnabled()) return;

        this.bytesRead += newBytesRead;
        if (this.bytesRead >= nextLevelToLogBytesRead)
        {
            logger.debug("Read " + this.bytesRead + " bytes and couting...");
            nextLevelToLogBytesRead += LOG_BYTE_INTERVAL;
        }
    }

}
