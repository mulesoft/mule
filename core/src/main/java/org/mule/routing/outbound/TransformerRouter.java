/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

/**
 * Simply applies a transformer before continuing on to the next router.
 * This can be useful with the {@link ChainingRouter}.
 * @deprecated
 */
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
                propagateMagicProperties(message, message);
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

