/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.mule.expression.ExpressionConstants.OPTIONAL_ARGUMENT;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.RequiredValueException;
import org.mule.config.i18n.CoreMessages;

import javax.activation.DataHandler;

/**
 * Looks up an attachment with the given name.
 *
 * @see MessageAttachmentsListExpressionEvaluator
 * @see MessageAttachmentsExpressionEvaluator
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageAttachmentExpressionEvaluator extends BaseAttachmentExpressionEvaluator
{
    public static final String NAME = "attachment";

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

    @Override
    protected DataHandler getAttachment(MuleMessage message, String attachmentName)
    {
        return message.getInboundAttachment(attachmentName);
    }
}
