/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.mule.umo.endpoint.UMOEndpointURI;

import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.PoolableObjectFactory;

public class FtpConnectionFactory implements PoolableObjectFactory
{
    private UMOEndpointURI uri;

    public FtpConnectionFactory(UMOEndpointURI uri)
    {
        this.uri = uri;
    }

    public Object makeObject() throws Exception
    {
        FTPClient client = new FTPClient();
        try
        {
            if (uri.getPort() > 0)
            {
                client.connect(uri.getHost(), uri.getPort());
            }
            else
            {
                client.connect(uri.getHost());
            }
            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }
            if (!client.login(uri.getUsername(), uri.getPassword()))
            {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }
            if (!client.setFileType(FTP.BINARY_FILE_TYPE))
            {
                throw new IOException("Ftp error. Couldn't set BINARY transfer type.");
            }
        }
        catch (Exception e)
        {
            if (client.isConnected())
            {
                client.disconnect();
            }
            throw e;
        }
        return client;
    }

    public void destroyObject(Object obj) throws Exception
    {
        FTPClient client = (FTPClient) obj;
        client.logout();
        client.disconnect();
    }

    public boolean validateObject(Object obj)
    {
        FTPClient client = (FTPClient) obj;
        try
        {
            client.sendNoOp();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public void activateObject(Object obj) throws Exception
    {
        FTPClient client = (FTPClient) obj;
        client.setReaderThread(true);
    }

    public void passivateObject(Object obj) throws Exception
    {
        FTPClient client = (FTPClient) obj;
        client.setReaderThread(false);
    }
}

