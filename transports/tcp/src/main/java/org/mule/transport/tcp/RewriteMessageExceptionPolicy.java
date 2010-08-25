/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;
import org.mule.transport.tcp.TcpMessageReceiver.TcpWorker;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An exception policy that rewrites the exception
 * 
 * @since 2.2.6
 */
public class RewriteMessageExceptionPolicy implements NextMessageExceptionPolicy
{
    
    private transient Log logger = LogFactory.getLog(getClass());

    /**
     * {@inheritDoc}
     * @throws Exception 
     */
    public Object handleException(Exception exception, TcpMessageReceiver receiver, TcpWorker worker) throws Exception
    {
        try
        {
            logger.warn("Protocol Read failed! " + exception);
            MuleMessage msg = new DefaultMuleMessage(NullPayload.getInstance(), receiver.getConnector().getMuleContext());
            ExceptionPayload exPayload = new DefaultExceptionPayload(exception);
            msg.setExceptionPayload(exPayload);
            List msgList = new ArrayList(1);
            msgList.add(msg);
            worker.handleResults(msgList);
            return null;
        }
        catch (Exception ex2)
        {
            logger.warn("Protocol Write failed! " + ex2);
            throw ex2;
        }
    }

}
