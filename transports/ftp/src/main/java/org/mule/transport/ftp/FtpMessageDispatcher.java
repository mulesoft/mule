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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;

public class FtpMessageDispatcher extends AbstractMessageDispatcher
{
    protected final FtpConnector connector;

    public FtpMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FtpConnector) endpoint.getConnector();
    }

    protected void doDispose()
    {
        // no op
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object data = event.transformMessage();
        OutputStream out = connector.getOutputStream(event.getEndpoint(), event.getMessage());

        try
        {
            if (data instanceof InputStream)
            {
                InputStream is = ((InputStream) data);
                IOUtils.copy(is, out);
                is.close();
            }
            else
            {
                byte[] dataBytes;
                if (data instanceof byte[])
                {
                    dataBytes = (byte[]) data;
                }
                else
                {
                    dataBytes = data.toString().getBytes(event.getEncoding());
                }
                IOUtils.write(dataBytes, out);
            }
        }
        finally
        {
            out.close();
        }
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return new DefaultMuleMessage(NullPayload.getInstance());
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        try
        {
            EndpointURI uri = endpoint.getEndpointURI();
            FTPClient client = connector.getFtp(uri);
            connector.destroyFtp(uri, client);
        }
        catch (Exception e)
        {
            // pool may be closed
        }
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        FTPClient client = null;
        try
        {
            client = connector.createFtpClient(endpoint);
            client.sendNoOp();
            client.logout();
            client.disconnect();

            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        finally
        {
            try
            {
                if (client != null)
                {
                    connector.releaseFtp(endpoint.getEndpointURI(), client);
                }
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed to release ftp client " + client, e);
                }

            }
        }

        return retryContext;
    }
}
