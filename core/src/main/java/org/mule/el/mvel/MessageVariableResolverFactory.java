/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleMessage;
import org.mule.api.el.VariableAssignmentCallback;
import org.mule.api.transport.PropertyScope;
import org.mule.el.context.MessageContext;
import org.mule.el.context.MessagePropertyMapContext;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mule.mvel2.integration.impl.SimpleValueResolver;

class MessageVariableResolverFactory extends ImmutableDefaultFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    private final String MESSAGE = "message";
    private final String PAYLOAD = "payload";
    private final String EXCEPTION = "exception";
    private final String FLOW_VARS = "flowVars";
    private final String SESSION_VARS = "sessionVars";

    private MuleMessage muleMessage;

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
                return new SimpleValueResolver(new MessageContext(muleMessage));
            }
            else if (PAYLOAD.equals(name))
            {
                return new MuleVariableResolver<Object>(PAYLOAD, muleMessage.getPayload(), null,
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
                return new SimpleValueResolver(new MessagePropertyMapContext(muleMessage,
                    PropertyScope.INVOCATION));
            }
            else if (EXCEPTION.equals(name))
            {
                if (muleMessage.getExceptionPayload() != null)
                {
                    return new SimpleValueResolver(muleMessage.getExceptionPayload().getException());
                }
                else
                {
                    return new SimpleValueResolver(null);
                }
            }
            else if (SESSION_VARS.equals(name))
            {
                return new SimpleValueResolver(new MessagePropertyMapContext(muleMessage,
                    PropertyScope.SESSION));
            }
            else if (MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE.equals(name))
            {
                return new SimpleValueResolver(muleMessage);
            }
        }
        return null;
    }

    @Override
    public boolean isResolveable(String name)
    {
        return isTarget(name);
    }

    public MessageVariableResolverFactory(MuleMessage message)
    {
        this.muleMessage = message;
    }
}
