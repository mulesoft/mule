/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;

/**
 * <code>MessagingException</code> is a general message exception thrown when
 * errors specific to Message processing occur..
 */

public class MessagingException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6941498759267936649L;

    /**
     * The MuleMessage being processed when the error occurred
     */
    protected final transient MuleMessage muleMessage;

    public MessagingException(Message message, MuleMessage muleMessage)
    {
        super();
        this.muleMessage = muleMessage;
        setMessage(generateMessage(message));
    }

    public MessagingException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(cause);
        this.muleMessage = muleMessage;
        setMessage(generateMessage(message));
    }

    private String generateMessage(Message message)
    {
        StringBuffer buf = new StringBuffer(80);

        if (message != null)
        {
            buf.append(message.getMessage()).append(". ");
        }

        if (muleMessage != null)
        {
            Object payload = muleMessage.getPayload();
            if (payload == null)
            {
                payload = NullPayload.getInstance();
            }

            buf.append(CoreMessages.messageIsOfType(payload.getClass()).getMessage());
            addInfo("Payload", StringUtils.abbreviate(payload.toString(), 1000));
        }
        else
        {
            buf.append("The current MuleMessage is null! Please report this to ").append(MuleManifest.getDevListEmail());
            addInfo("Payload", NullPayload.getInstance().toString());
        }

        return buf.toString();
    }

    public MuleMessage getMuleMessage()
    {
        return muleMessage;
    }

}
