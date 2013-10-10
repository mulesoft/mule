/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public class AddOutboundAttachments extends AbstractMessageTransformer
{
    @Override
    public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
    {
        try
        {
            msg.addOutboundAttachment("seeya", "seeya", "application/text");
            msg.addOutboundAttachment("goodbye", "goodbye", "application/xml");
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
        return msg.getPayload();
    }
}

