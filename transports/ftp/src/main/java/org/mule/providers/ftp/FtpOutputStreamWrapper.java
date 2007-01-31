/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.ftp;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;

/**
 * TODO
 */
public class FtpOutputStreamWrapper extends OutputStream
{
    private final FTPClient client;
    private final OutputStream out;

    public FtpOutputStreamWrapper(FTPClient client, OutputStream out)
    {
        this.client = client;
        this.out = out;
    }

    public void write(int b) throws IOException
    {
        out.write(b);
    }

    public void write(byte b[]) throws IOException
    {
        out.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        out.write(b, off, len);
    }

    public void flush() throws IOException
    {
        out.flush();
    }

    public void close() throws IOException
    {
        try
        {
            // close output stream
            out.close();

            if (!client.completePendingCommand())
            {
                client.logout();
                client.disconnect();
                throw new IOException("FTP Stream failed to complete pending request");
            }
        }
        finally
        {
            out.close();
            super.close();
        }
    }

    FTPClient getFtpClient()
    {
        return client;
    }
}