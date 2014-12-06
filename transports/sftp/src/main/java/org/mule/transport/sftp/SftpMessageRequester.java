/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.sftp.notification.SftpNotifier;

import java.io.InputStream;
import java.util.List;

/**
 * <code>SftpMessageRequester</code> polls files on request (e.g. from a
 * quartz-inbound-endpoint) from an sftp service on request using jsch. This
 * requester produces an InputStream payload, which can be materialized in a
 * MessageDispatcher or Component.
 */
public class SftpMessageRequester extends AbstractMessageRequester
{

    private SftpReceiverRequesterUtil sftpRRUtil = null;

    public SftpMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);

        sftpRRUtil = new SftpReceiverRequesterUtil(endpoint);

    }

    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        final List<FileDescriptor> files = sftpRRUtil.getAvailableFiles(true);

        if (files.isEmpty()) return null;

        String path = files.get(0).getFilename();
        // TODO. ML FIX. Can't we figure out the current service (for logging/audit
        // purpose)???
        SftpNotifier notifier = new SftpNotifier((SftpConnector) connector, createNullMuleMessage(),
            endpoint, endpoint.getName());

        InputStream inputStream = sftpRRUtil.retrieveFile(path, notifier);

        logger.debug("Routing file: " + path);

        MuleMessage message = createMuleMessage(inputStream);
        message.setProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path, PropertyScope.INBOUND);

        // Now we can update the notifier with the message
        notifier.setMessage(message);
        return message;
    }

}
