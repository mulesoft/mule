/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
