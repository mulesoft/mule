
/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * Simply applies a transformer before continuing on to the next router.
 * This can be useful with the {@link ChainingRouter}.
 */
public class TransformerRouter extends AbstractOutboundRouter
{
    private UMOTransformer transformer;

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws MessagingException
    {
        if (transformer != null)
        {
            try
            {
                Object payload = transformer.transform(message.getPayload());
                message = new MuleMessage(payload, message);
            }
            catch (TransformerException e)
            {
                throw new RoutingException(
                    CoreMessages.transformFailedBeforeFilter(),
                    message, (UMOEndpoint)endpoints.get(0), e);
            }
        }
        return message;
    }

    public boolean isMatch(UMOMessage message) throws MessagingException
    {
        return true;
    }

    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    public void setTransformer(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }
}

