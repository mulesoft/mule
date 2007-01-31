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

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpMessageDispatcher extends AbstractMessageDispatcher
{
    protected final FtpConnector connector;

    public FtpMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FtpConnector)endpoint.getConnector();
    }

    protected void doDispose()
    {
        // no op
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        FTPClient client = null;
        UMOEndpointURI uri = event.getEndpoint().getEndpointURI();

        try
        {
            Object data = event.getTransformedMessage();
            byte[] dataBytes;

            if (data instanceof byte[])
            {
                dataBytes = (byte[])data;
            }
            else
            {
                dataBytes = data.toString().getBytes();
            }

            FtpOutputStreamWrapper out = (FtpOutputStreamWrapper)connector.getOutputStream(event.getEndpoint(),
                event.getMessage());
            client = out.getFtpClient();
            IOUtils.write(dataBytes, out);
            // This will ensure that the completePendingRequest is called
            out.close();
        }
        finally
        {
            connector.releaseFtp(uri, client);
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    protected void doConnect() throws Exception
    {
        FTPClient client = connector.getFtp(endpoint.getEndpointURI());
        connector.releaseFtp(endpoint.getEndpointURI(), client);
    }

    protected void doDisconnect() throws Exception
    {
        FTPClient client = connector.getFtp(endpoint.getEndpointURI());
        connector.destroyFtp(endpoint.getEndpointURI(), client);
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(long timeout) throws Exception
    {
        FTPClient client = null;

        try
        {

            client = connector.getFtp(endpoint.getEndpointURI());
            // not sure this getParams() will always work, there's a TODO in the code
            connector.enterActiveOrPassiveMode(client, endpoint);
            connector.setupFileType(client, endpoint);
            if (!client.changeWorkingDirectory(endpoint.getEndpointURI().getPath()))
            {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }

            FilenameFilter filenameFilter = null;
            if (endpoint.getFilter() instanceof FilenameFilter)
            {
                filenameFilter = (FilenameFilter)endpoint.getFilter();
            }

            FTPFile[] files = client.listFiles();
            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }
            if (files == null || files.length == 0)
            {
                return null;
            }
            List fileList = new ArrayList();
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isFile())
                {
                    if (filenameFilter == null || filenameFilter.accept(null, files[i].getName()))
                    {
                        fileList.add(files[i]);
                        // only read the first one
                        break;
                    }
                }
            }
            if (fileList.size() == 0)
            {
                return null;
            }

            FTPFile file = (FTPFile)fileList.get(0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!client.retrieveFile(file.getName(), baos))
            {
                throw new IOException("Ftp error: " + client.getReplyCode());
            }
            return new MuleMessage(connector.getMessageAdapter(baos.toByteArray()));

        }
        finally
        {
            connector.releaseFtp(endpoint.getEndpointURI(), client);
        }
    }

}
