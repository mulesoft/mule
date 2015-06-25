/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates a Map facade around a {@link org.mule.api.MuleMessage} instance to allow access to outbound
 * attachments from within components and transformers without the these objects needing access to the Mule Message
 */
public class OutboundAttachmentsExpressionEvaluator extends AbstractExpressionEvaluator
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
