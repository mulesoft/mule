/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.notification;

import java.util.Date;

import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.transport.sftp.notification.SftpTransportNotification;
import org.mule.transport.sftp.notification.SftpTransportNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_GET_ACTION_MSG;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_PUT_ACTION_MSG;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_RENAME_ACTION_MSG;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_DELETE_ACTION_MSG;

public class SftpTransportNotificationTestListener implements SftpTransportNotificationListener
{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static boolean gotSftpPutNotification = false;
    private static boolean gotSftpRenameNotification = false;
    private static boolean gotSftpGetNotification = false;
    private static boolean gotSftpDeleteNotification = false;

    public void onNotification(ServerNotification notification)
    {

        SftpTransportNotification sftpNotification;
        if (notification instanceof SftpTransportNotification)
        {
            sftpNotification = (SftpTransportNotification) notification;
        }
        else
        {
            logger.debug("SftpTransportNotificationTestListener RECEIVED UNKNOWN NOTIFICATION OF TYPE {}",
                notification.getClass().getName());
            return;
        }

        String action = notification.getActionName();

        if (action.equals(SFTP_GET_ACTION_MSG))
        {
            gotSftpGetNotification = true;

        }
        else if (action.equals(SFTP_PUT_ACTION_MSG))
        {
            gotSftpPutNotification = true;

        }
        else if (action.equals(SFTP_RENAME_ACTION_MSG))
        {
            gotSftpRenameNotification = true;

        }
        else if (action.equals(SFTP_DELETE_ACTION_MSG))
        {
            gotSftpDeleteNotification = true;
        }

        String resourceId = notification.getResourceIdentifier();
        String timestamp = new Date(notification.getTimestamp()).toString();

        String endpoint = sftpNotification.getEndpoint().getEndpointURI().toString();
        String info = sftpNotification.getInfo();
        long size = sftpNotification.getSize();

        String msgType = "???";
        String correlationId = "???";
        if (notification.getSource() instanceof MuleMessage)
        {
            MuleMessage message = (MuleMessage) notification.getSource();
            msgType = message.getPayload().getClass().getName();
            correlationId = (String) message.getProperty("MULE_CORRELATION_ID", "?");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("OnNotification: " + notification.EVENT_NAME + "\nAction=" + action + " " + info
                         + " " + size + "\nEndpoint=" + endpoint + "\nTimestamp=" + timestamp + "\nMsgType="
                         + msgType + "\nResourceId=" + resourceId + "\nCorrelationId=" + correlationId + "");
        }
    }

    public static void reset()
    {
        gotSftpPutNotification = false;
        gotSftpRenameNotification = false;
        gotSftpGetNotification = false;
        gotSftpDeleteNotification = false;
    }

    public static boolean gotSftpPutNotification()
    {
        return gotSftpPutNotification;
    }

    public static boolean gotSftpRenameNotification()
    {
        return gotSftpRenameNotification;
    }

    public static boolean gotSftpGetNotification()
    {
        return gotSftpGetNotification;
    }

    public static boolean gotSftpDeleteNotification()
    {
        return gotSftpDeleteNotification;
    }

}
