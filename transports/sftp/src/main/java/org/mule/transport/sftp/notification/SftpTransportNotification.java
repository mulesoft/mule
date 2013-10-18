/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.context.notification.CustomNotification;
import org.mule.context.notification.EndpointMessageNotification;

public class SftpTransportNotification extends CustomNotification
{

    /**
     * serial version
     */
    private static final long serialVersionUID = 4662315555948069782L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(EndpointMessageNotification.class);

    /**
     * sftp transport specific actions
     */
    private static final int SFTP_ACTION_BASE = CUSTOM_EVENT_ACTION_START_RANGE * 2;
    public static final int SFTP_GET_ACTION = SFTP_ACTION_BASE + 1;
    public static final int SFTP_PUT_ACTION = SFTP_ACTION_BASE + 2;
    public static final int SFTP_RENAME_ACTION = SFTP_ACTION_BASE + 3;
    public static final int SFTP_DELETE_ACTION = SFTP_ACTION_BASE + 4;

    public static final String SFTP_GET_ACTION_MSG = "sftp.get";
    public static final String SFTP_PUT_ACTION_MSG = "sftp.put";
    public static final String SFTP_RENAME_ACTION_MSG = "sftp.rename";
    public static final String SFTP_DELETE_ACTION_MSG = "sftp.delete";

    /**
     * sftp transport specific information
     */
    private ImmutableEndpoint endpoint;
    private String info;
    private long size;

    static
    {
        registerAction(SFTP_GET_ACTION_MSG, SFTP_GET_ACTION);
        registerAction(SFTP_PUT_ACTION_MSG, SFTP_PUT_ACTION);
        registerAction(SFTP_RENAME_ACTION_MSG, SFTP_RENAME_ACTION);
        registerAction(SFTP_DELETE_ACTION_MSG, SFTP_DELETE_ACTION);
    }

    public SftpTransportNotification(MuleMessage resource,
                                     ImmutableEndpoint endpoint,
                                     String resourceIdentifier,
                                     int action,
                                     String info,
                                     long size)
    {

        super(resource, action, resourceIdentifier);

        if (logger.isDebugEnabled()) logger.debug("*** SftpTransportNotification object created ***");

        this.endpoint = endpoint;
        this.info = info;
        this.size = size;
    }

    protected String getPayloadToString()
    {
        try
        {
            return ((MuleMessage) source).getPayloadAsString();
        }
        catch (Exception e)
        {
            return source.toString();
        }
    }

    public String toString()
    {
        return EVENT_NAME + "{action = " + getActionName(action) + ", endpoint = "
               + endpoint.getEndpointURI() + ", info = " + info + ", size = " + size
               + ", resourceIdentifier = " + resourceIdentifier + ", timestamp = " + timestamp
               + ", serverId = " + serverId + ", message = " + source + "}";
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public String getInfo()
    {
        return info;
    }

    public long getSize()
    {
        return size;
    }

}
