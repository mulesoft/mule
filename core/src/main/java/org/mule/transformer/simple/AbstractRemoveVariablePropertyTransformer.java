/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;
import org.mule.util.WildcardAttributeEvaluator;

public abstract class AbstractRemoveVariablePropertyTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator identifierEvaluator;
    private WildcardAttributeEvaluator wildcardAttributeEvaluator;

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
    public Object transformMessage(final MuleMessage message, String outputEncoding) throws TransformerException
    {
        if (wildcardAttributeEvaluator.hasWildcards())
        {
            wildcardAttributeEvaluator.processValues(message.getPropertyNames(getScope()),new WildcardAttributeEvaluator.MatchCallback()
            {
                @Override
                public void processMatch(String matchedValue)
                {
                    message.removeProperty(matchedValue,getScope());
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(String.format("Removing property: '%s' from scope: '%s'", matchedValue, getScope().getScopeName()));
                    }
                }
            });    
        }
        else
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
        this.identifierEvaluator = new AttributeEvaluator(identifier);
        this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(identifier);
    }

    public abstract PropertyScope getScope();
}
