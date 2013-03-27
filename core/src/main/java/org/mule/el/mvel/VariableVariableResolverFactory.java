/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;

import org.mvel2.ParserContext;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;

public class VariableVariableResolverFactory extends MVELExpressionLanguageContext
{

    private static final long serialVersionUID = -4433478558175131280L;

    private MuleMessage message;

    public VariableVariableResolverFactory(ParserContext parserContext,
                                           MuleContext muleContext,
                                           MuleEvent event)
    {
        super(parserContext, muleContext);
        this.message = event.getMessage();
    }

    @Deprecated
    public VariableVariableResolverFactory(ParserContext parserContext,
                                           MuleContext muleContext,
                                           MuleMessage message)
    {
        super(parserContext, muleContext);
        this.message = message;
    }

    @SuppressWarnings("deprecation")
    public boolean isTarget(String name)
    {
        if (message == null)
        {
            return isNextResolveable(name);
        }
        return message.getInvocationPropertyNames().contains(name)
               || message.getSessionPropertyNames().contains(name) || isNextResolveable(name);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VariableResolver getVariableResolver(String name)
    {

        if (message != null && message.getInvocationPropertyNames().contains(name))
        {
            return new FlowVariableVariableResolver(name);
        }
        else if (message != null && message.getSessionPropertyNames().contains(name))
        {
            return new SessionVariableVariableResolver(name);
        }
        else if (nextFactory != null)
        {
            return nextFactory.getVariableResolver(name);
        }
        else
        {
            throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
        }
    }

    @SuppressWarnings("rawtypes")
    class FlowVariableVariableResolver implements VariableResolver
    {

        private static final long serialVersionUID = -4847663330454657440L;

        String name;

        public FlowVariableVariableResolver(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public Class getType()
        {
            return Object.class;
        }

        @Override
        public void setStaticType(Class type)
        {
        }

        @Override
        public int getFlags()
        {
            return 0;
        }

        @Override
        public Object getValue()
        {
            return message.getInvocationProperty(name);
        }

        @Override
        public void setValue(Object value)
        {
            message.setInvocationProperty(name, value);
        }
    }

    @SuppressWarnings({"deprecation", "rawtypes"})
    class SessionVariableVariableResolver implements VariableResolver
    {

        private static final long serialVersionUID = 7658449705305592397L;

        private String name;

        public SessionVariableVariableResolver(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public Class getType()
        {
            return Object.class;
        }

        @Override
        public void setStaticType(Class type)
        {
        }

        @Override
        public int getFlags()
        {
            return 0;
        }

        @Override
        public Object getValue()
        {
            return message.getSessionProperty(name);
        }

        @Override
        public void setValue(Object value)
        {
            message.setSessionProperty(name, value);
        }
    }

    @SuppressWarnings("rawtypes")
    class RegistryVariableVariableResolver implements VariableResolver
    {

        private static final long serialVersionUID = 7658449705305592397L;

        String name;

        public RegistryVariableVariableResolver(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public Class getType()
        {
            return Object.class;
        }

        @Override
        public void setStaticType(Class type)
        {
        }

        @Override
        public int getFlags()
        {
            return 0;
        }

        @Override
        public Object getValue()
        {
            Object value = message.getMuleContext().getRegistry().lookupObject(name);
            if (value != null)
            {
                return value;
            }
            else
            {
                throw new UnresolveablePropertyException(name);
            }
        }

        @Override
        public void setValue(Object value)
        {
            throw new UnsupportedOperationException();
        }
    }

}
