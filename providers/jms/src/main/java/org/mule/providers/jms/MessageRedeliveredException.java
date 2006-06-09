/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.jms;

import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MessageRedeliveredException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 9013890402770563931L;

    public MessageRedeliveredException(JmsMessageAdapter jmsMessage)
    {
        super(new org.mule.config.i18n.Message("jms", 7, (jmsMessage == null ? "[null message]"
                : jmsMessage.getUniqueId())), jmsMessage);
    }

    public MessageRedeliveredException(Message message, JmsMessageAdapter jmsMessage)
    {
        super(message.setNextMessage(new org.mule.config.i18n.Message("jms", 7, (jmsMessage == null ? "[null message]"
                : jmsMessage.getUniqueId()))), jmsMessage);
    }
}
