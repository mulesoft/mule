/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates a Map facade around a {@link org.mule.api.MuleMessage} instance to allow access to outbound
 * attachments from within components and transformers without the these objects needing access to the Mule Message
 */
public class OutboundAttachmentsExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "outboundAttachments";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(OutboundAttachmentsExpressionEvaluator.class);


    public Object evaluate(String expression, MuleMessage message)
    {
        if (message == null)
        {
            return null;
        }
        return new OutboundAttachmentsMap(message);
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("name");
    }
}
