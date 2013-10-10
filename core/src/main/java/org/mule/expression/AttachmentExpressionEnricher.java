/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import javax.activation.DataHandler;

import java.text.MessageFormat;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.expression.ExpressionEnricher;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;

public class AttachmentExpressionEnricher implements ExpressionEnricher
{

    public static final String NAME = "attachment";

    public void enrich(String expression, MuleMessage message, Object object)
    {
        if (object instanceof DataHandler)
        {
            try
            {
                message.addOutboundAttachment(expression, (DataHandler) object);
            } catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        else
        {
            String[] split = expression.split(ExpressionConstants.DELIM);
            if (split.length < 2)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(MessageFormat.format("attachment enrichment expression {0} does not declare a content type",expression)));
            }
            String attachmentName = split[0];
            String contentType = split[1];
            try
            {
                message.addOutboundAttachment(attachmentName,object,contentType);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("failed to set attachment"));
            }
        }
    }

    public String getName()
    {
        return NAME;
    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

}
