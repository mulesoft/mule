/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.el.VariableAssignmentCallback;
import org.mule.PropertyScope;
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

    private MuleEvent event;
    private MuleContext muleContext;

    public MessageVariableResolverFactory(final ParserConfiguration parserConfiguration,
                                          final MuleContext muleContext,
                                          final MuleEvent event)
    {
        this.event = event;
        this.muleContext = muleContext;
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
                                          final MuleEvent event,
                                          final VariableResolverFactory next)
    {
        this(parserConfiguration, muleContext, event);
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
        if (event != null)
        {
            if (MESSAGE.equals(name))
            {
                return new MuleImmutableVariableResolver<MessageContext>(MESSAGE, new MessageContext(
                        event, muleContext), null);
            }
            else if (PAYLOAD.equals(name))
            {
                return new MuleVariableResolver<Object>(PAYLOAD, new MessageContext(
                        event, muleContext).getPayload(), null,
                    new VariableAssignmentCallback<Object>()
                    {
                        @Override
                        public void assignValue(String name, Object value, Object newValue)
                        {
                            event.setMessage(new DefaultMuleMessage(newValue, event.getMessage(), event.getMuleContext()));
                        }
                    });
            }
            else if (FLOW_VARS.equals(name))
            {
                return new MuleImmutableVariableResolver<Map<String, Object>>(FLOW_VARS,
                    new MessagePropertyMapContext(event, PropertyScope.INVOCATION), null);
            }
            else if (EXCEPTION.equals(name))
            {
                if (event.getMessage().getExceptionPayload() != null)
                {
                    return new MuleImmutableVariableResolver<Throwable>(EXCEPTION,
                        event.getMessage().getExceptionPayload().getException(), null);
                }
                else
                {
                    return new MuleImmutableVariableResolver<MuleMessage>(EXCEPTION, null, null);
                }
            }
            else if (SESSION_VARS.equals(name))
            {
                return new MuleImmutableVariableResolver<Map<String, Object>>(SESSION_VARS,
                    new MessagePropertyMapContext(event, PropertyScope.SESSION), null);
            }
            else if (MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name))
            {
                return new MuleImmutableVariableResolver<MuleMessage>(
                    MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE, event.getMessage(), null);
            }
        }
        return super.getNextFactoryVariableResolver(name);
    }

}
