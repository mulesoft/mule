/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;

/**
 * Simply applies a transformer before continuing on to the next router.
 * This can be useful with the {@link ChainingRouter}.
 */
public class TransformerRouter extends AbstractOutboundRouter
{
    private Transformer transformer;

    public MuleMessage route(MuleMessage message, MuleSession session) throws MessagingException
    {
        if (transformer != null)
        {
            try
            {
                Object payload = transformer.transform(message.getPayload());
                message = new DefaultMuleMessage(payload, message);
            }
            catch (TransformerException e)
            {
                throw new RoutingException(
                    CoreMessages.transformFailedBeforeFilter(),
                    message, (ImmutableEndpoint)endpoints.get(0), e);
            }
        }
        return message;
    }

    public boolean isMatch(MuleMessage message) throws MessagingException
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

