/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.AbstractRoutingStrategy;

/**
 * Simply applies a transformer before continuing on to the next router.
 * This can be useful with the {@link ChainingRouter}.
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
public class TransformerRouter extends AbstractOutboundRouter
{
    private Transformer transformer;

    @Override
    public MuleEvent route(MuleEvent event) throws MessagingException
    {
        if (event == null || VoidMuleEvent.getInstance().equals(event))
        {
            return event;
        }
        
        MuleMessage message = event.getMessage();

        if (transformer != null)
        {
            try
            {
                Object payload = transformer.transform(message.getPayload());
                message = new DefaultMuleMessage(payload, message, muleContext);
                AbstractRoutingStrategy.propagateMagicProperties(message,message);
            }
            catch (TransformerException e)
            {
                throw new RoutingException(CoreMessages.transformFailedBeforeFilter(), event, 
                    routes.get(0), e);
            }
        }
        return message == null ? null : new DefaultMuleEvent(message, event);
    }

    public boolean isMatch(MuleMessage message) throws MuleException
    {
        return true;
    }

    public Transformer getTransformer()
    {
        return transformer;
    }

    public void setTransformer(Transformer transformer)
    {
        this.transformer = transformer;
    }
}

