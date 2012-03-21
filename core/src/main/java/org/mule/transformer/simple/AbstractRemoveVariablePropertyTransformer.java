/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import java.util.HashSet;
import java.util.Set;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

public abstract class AbstractRemoveVariablePropertyTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator identifierEvaluator;

    public AbstractRemoveVariablePropertyTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        this.identifierEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        if (identifierEvaluator.isPlainText() || identifierEvaluator.isExpression())
        {
            Object keyValue = identifierEvaluator.resolveValue(message);
            if (keyValue != null)
            {
                message.removeProperty(keyValue.toString(), getScope());
            }
            else
            {
                logger.info("Key expression return null, no property will be removed");
            }
        }
        else
        {
            final Set<String> propertyNames = new HashSet<String>(message.getPropertyNames(getScope()));
            for (String key : propertyNames)
            {
                if (identifierEvaluator.matches(key))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Removing property: '%s' from scope: '%s'", key, getScope().getScopeName()));
                    }
                    message.removeProperty(key, getScope());
                }
            }
        }
        return message;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        AbstractRemoveVariablePropertyTransformer clone = (AbstractRemoveVariablePropertyTransformer) super.clone();
        clone.setIdentifier(this.identifierEvaluator.getRawValue());
        return clone;
    }

    public void setIdentifier(String identifier)
    {
        if (identifier == null)
        {
            throw new IllegalArgumentException("Remove with null identifier is not supported");
        }
        this.identifierEvaluator = new AttributeEvaluator(identifier).enableRegexSupport();
    }

    public abstract PropertyScope getScope();
}
