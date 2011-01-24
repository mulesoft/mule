/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.sftp.notification.SftpNotifier;

import java.io.InputStream;
import java.util.Arrays;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp service
 * using jsch. This receiver produces an InputStream payload, which can be
 * materialized in a MessageDispatcher or Component.
 */
public class SftpMessageReceiver extends AbstractPollingMessageReceiver
{

    private SftpReceiverRequesterUtil sftpRRUtil = null;

    public SftpMessageReceiver(SftpConnector connector,
                               SimpleFlowConstruct flow,
                               InboundEndpoint endpoint,
                               long frequency) throws CreateException
    {
        super(connector, flow, endpoint);

        this.setFrequency(frequency);

        sftpRRUtil = new SftpReceiverRequesterUtil(endpoint);
    }

    public SftpMessageReceiver(SftpConnector connector, FlowConstruct flow, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flow, endpoint);
        sftpRRUtil = new SftpReceiverRequesterUtil(endpoint);
    }

    public void poll() throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Pooling. Called at endpoint " + endpoint.getEndpointURI());
        }
        try
        {
            String[] files = sftpRRUtil.getAvailableFiles(false);

            if (files.length == 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Pooling. No matching files found at endpoint " + endpoint.getEndpointURI());
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Pooling. " + files.length + " files found at " + endpoint.getEndpointURI()
                                 + ":" + Arrays.toString(files));
                }
                for (String file : files)
                {
                    routeFile(file);
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("Pooling. Routed all " + files.length + " files found at "
                                 + endpoint.getEndpointURI());
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Error in poll", e);
            throw e;
        }
    }

    protected void routeFile(String path) throws Exception
    {
        // A bit tricky initialization of the notifier in this case since we don't
        // have access to the message yet...
        SftpNotifier notifier = new SftpNotifier((SftpConnector) connector, createNullMuleMessage(),
            endpoint, flowConstruct.getName());

        InputStream inputStream = sftpRRUtil.retrieveFile(path, notifier);

        if (logger.isDebugEnabled())
        {
            logger.debug("Routing file: " + path);
        }

        MuleMessage message = createMuleMessage(inputStream);

        message.setOutboundProperty(SftpConnector.PROPERTY_FILENAME, path);
        message.setOutboundProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path);

        // Now we have access to the message, update the notifier with the message
        notifier.setMessage(message);
        routeMessage(message);

        if (logger.isDebugEnabled())
        {
            logger.debug("Routed file: " + path);
        }
    }

    /**
     * SFTP-35
     * @param message
     * @return
     */
    @Override    
    protected MuleMessage handleUnacceptedFilter(MuleMessage message) {
        logger.debug("the filter said no, now trying to close the payload stream");
        try {
            final SftpInputStream payload = (SftpInputStream) message.getPayload();
            payload.close();
        }
        catch (Exception e) {
            logger.debug("unable to close payload stream", e);
        }
        return super.handleUnacceptedFilter(message);
    }
    
    public void doConnect() throws Exception
    {
        // no op
    }

    public void doDisconnect() throws Exception
    {
        // no op
    }

    protected void doDispose()
    {
        // no op
    }
}
