/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;

public class MessageRedeliveredException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 9013890402770563931L;

    public MessageRedeliveredException(JmsMessageAdapter jmsMessage)
    {
        super(new org.mule.config.i18n.Message("jms", 7, (jmsMessage == null
                        ? "[null message]" : jmsMessage.getUniqueId())), jmsMessage);
    }

    public MessageRedeliveredException(Message message, JmsMessageAdapter jmsMessage)
    {
        super(message.setNextMessage(new org.mule.config.i18n.Message("jms", 7, (jmsMessage == null
                        ? "[null message]" : jmsMessage.getUniqueId()))), jmsMessage);
    }
}
