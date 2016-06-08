/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.connector;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_SYNC_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultReplyToHandler</code> is responsible for processing a message
 * replyTo header.
 */

public class DefaultReplyToHandler implements ReplyToHandler, Serializable, DeserializationPostInitialisable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected transient MuleContext muleContext;
    protected transient Map<String, Object> serializedData = null;

    public DefaultReplyToHandler(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void processReplyTo(final MuleEvent event, final MuleMessage returnMessage, final Object replyTo) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("sending reply to: " + replyTo);
        }

        // make sure remove the replyTo property as not cause a a forever
        // replyto loop
        returnMessage.removeOutboundProperty(MULE_REPLY_TO_PROPERTY);
        event.removeFlowVariable(MULE_REPLY_TO_PROPERTY);

        // MULE-4617. This is fixed with MULE-4620, but lets remove this property
        // anyway as it should never be true from a replyTo dispatch
        returnMessage.removeOutboundProperty(MULE_REMOTE_SYNC_PROPERTY);
        event.removeFlowVariable(MULE_REMOTE_SYNC_PROPERTY);

        //TODO See MULE-9307 - re-add behaviour to process reply to destination dispatching with new connectors
    }

    @Override
    public void processExceptionReplyTo(MessagingException exception, Object replyTo)
    {
       // DefaultReplyToHandler does not send a reply message when an exception errors, this is rather handled by
       // using an exception strategy.
    }

    public void initAfterDeserialisation(MuleContext context) throws MuleException
    {
        // this method can be called even on objects that were not serialized. In this case,
        // the temporary holder for serialized data is not initialized and we can just return
        if (serializedData == null)
        {
            return;
        }
        this.muleContext = context;

        logger = LogFactory.getLog(getClass());
        serializedData = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();

        String connectorName = null;
        String connectorType = null;

        //Can be null if service call originates from MuleClient
        if (serializedData != null)
        {
            connectorName = (String) serializedData.get("connectorName");
            connectorType = (String) serializedData.get("connectorType");
        }
        else
        {
            //TODO See MULE-9307 - add behaviour to store config name to be used for reply to destination
        }
        out.writeObject(connectorName);
        out.writeObject(connectorType);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        serializedData = new HashMap<String, Object>();

        serializedData.put("connectorName", in.readObject());
        serializedData.put("connectorType", in.readObject());
    }
}
