/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.expression;

import org.mule.api.transport.MessageAdapter;

import javax.activation.DataHandler;

/**
 * Looks up an attachment with the given name.
 *
 * @see org.mule.util.expression.MessageAttachmentsListExpressionEvaluator
 * @see org.mule.util.expression.MessageAttachmentsExpressionEvaluator
 * @see org.mule.util.expression.ExpressionEvaluator
 * @see org.mule.util.expression.ExpressionEvaluatorManager
 */
public class MessageAttachmentExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "attachment";

    public Object evaluate(String name, MessageAdapter message)
    {
        if (message instanceof MessageAdapter)
        {
            DataHandler dh = ((MessageAdapter) message).getAttachment(name);
            return dh;
        }
        return null;
    }

    /** {@inheritDoc} */
    public String getName()
    {
        return NAME;
    }

    /** {@inheritDoc} */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}