/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.api.endpoint.EndpointURI;

import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.PoolableObjectFactory;

public class FtpConnectionFactory implements PoolableObjectFactory
{
    private EndpointURI uri;

    public FtpConnectionFactory(EndpointURI uri)
    {
        this.uri = uri;
    }

    public Object makeObject() throws Exception
    {
        FTPClient client = new FTPClient();
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
            throw new IOException("Ftp connect failed: " + client.getReplyCode());
        }
        if (!client.login(uri.getUser(), uri.getPassword()))
        {
            throw new IOException("Ftp login failed: " + client.getReplyCode());
        }
        if (!client.setFileType(FTP.BINARY_FILE_TYPE))
        {
            throw new IOException("Ftp error. Couldn't set BINARY transfer type: " + client.getReplyCode());
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
        // nothing to do
    }

    public void passivateObject(Object obj) throws Exception
    {
        // nothing to do
    }
}

