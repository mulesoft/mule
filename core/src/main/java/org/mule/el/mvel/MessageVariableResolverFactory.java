/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.el.VariableAssignmentCallback;
import org.mule.api.transport.PropertyScope;
import org.mule.el.context.MessageContext;
import org.mule.el.context.MessagePropertyMapContext;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;

import java.util.Map;

public class MessageVariableResolverFactory extends MuleBaseVariableResolverFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    private static final String MESSAGE = "message";
    private static final String EXCEPTION = "exception";
    public static final String PAYLOAD = "payload";
    public static final String MESSAGE_PAYLOAD = MESSAGE + "." + PAYLOAD;
    public static final String FLOW_VARS = "flowVars";
    public static final String SESSION_VARS = "sessionVars";

    private MuleMessage muleMessage;

    public MessageVariableResolverFactory(final ParserConfiguration parserConfiguration,
                                          final MuleContext muleContext,
                                          final MuleMessage message)
    {
        this.muleMessage = message;
    }

    /**
     * Convenience constructor to allow for more concise creation of VariableResolverFactory chains without
     * and performance overhead incurred by using a builder.
     * 
     * @param delegate
     * @param next
     */
    public MessageVariableResolverFactory(final ParserConfiguration parserConfiguration,
                                          final MuleContext muleContext,
                                          final MuleMessage message,
                                          final VariableResolverFactory next)
    {
        this(parserConfiguration, muleContext, message);
        setNextFactory(next);
    }

    @Override
    public boolean isTarget(String name)
    {
        return MESSAGE.equals(name) || PAYLOAD.equals(name) || FLOW_VARS.equals(name)
               || EXCEPTION.equals(name) || SESSION_VARS.equals(name)
               || MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name);
    }

    @Override
    public VariableResolver getVariableResolver(String name)
    {
        if (muleMessage != null)
        {
            if (MESSAGE.equals(name))
            {
                return new MuleImmutableVariableResolver<MessageContext>(MESSAGE, new MessageContext(
                    muleMessage), null);
            }
            else if (PAYLOAD.equals(name))
            {
                return new MuleVariableResolver<Object>(PAYLOAD, new MessageContext(
                        muleMessage).getPayload(), null,
                    new VariableAssignmentCallback<Object>()
                    {
                        @Override
                        public void assignValue(String name, Object value, Object newValue)
                        {
                            muleMessage.setPayload(newValue);
                        }
                    });
            }
            else if (FLOW_VARS.equals(name))
            {
                return new MuleImmutableVariableResolver<Map<String, Object>>(FLOW_VARS,
                    new MessagePropertyMapContext(muleMessage, PropertyScope.INVOCATION), null);
            }
            else if (EXCEPTION.equals(name))
            {
                if (muleMessage.getExceptionPayload() != null)
                {
                    return new MuleImmutableVariableResolver<Throwable>(EXCEPTION,
                        muleMessage.getExceptionPayload().getException(), null);
                }
                else
                {
                    return new MuleImmutableVariableResolver<MuleMessage>(EXCEPTION, null, null);
                }
            }
            else if (SESSION_VARS.equals(name))
            {
                return new MuleImmutableVariableResolver<Map<String, Object>>(SESSION_VARS,
                    new MessagePropertyMapContext(muleMessage, PropertyScope.SESSION), null);
            }
            else if (MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name))
            {
                return new MuleImmutableVariableResolver<MuleMessage>(
                    MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE, muleMessage, null);
            }
        }
        return super.getNextFactoryVariableResolver(name);
    }

}
