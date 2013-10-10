/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

