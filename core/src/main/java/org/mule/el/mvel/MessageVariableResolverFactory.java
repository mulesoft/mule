/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import org.mvel2.ParserContext;

public class MessageVariableResolverFactory extends AbstractVariableResolverFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    public MessageVariableResolverFactory(ParserContext parserContext,
                                          MuleContext muleContext,
                                          MuleMessage message)
    {
        super(parserContext, muleContext);

        if (message != null)
        {
            // Message / Payload
            addFinalVariable("message", message);
            addResolver("payload", new PayloadVariableResolver(message));

            // Only add exception is present
            if (message.getExceptionPayload() != null)
            {
                addFinalVariable("exception", message.getExceptionPayload().getException());
            }

            // Property, variable and attachment maps
            addFinalVariable("inbound", new MessagePropertyWrapperMap(message, PropertyScope.INBOUND));
            addFinalVariable("outbound", new MessagePropertyWrapperMap(message, PropertyScope.OUTBOUND));
            addFinalVariable("flowVariables",
                new MessagePropertyWrapperMap(message, PropertyScope.INVOCATION));
            addFinalVariable("sessionVariables",
                new MessagePropertyWrapperMap(message, PropertyScope.SESSION));
            addFinalVariable("inboundAttachments", new InboundAttachmentWrapperMap(message));
            addFinalVariable("outboundAttachments", new OutboundAttachmentWrapperMap(message));
        }
    }
}
