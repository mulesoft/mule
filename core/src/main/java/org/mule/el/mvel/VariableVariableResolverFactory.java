/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;

public class VariableVariableResolverFactory extends MuleBaseVariableResolverFactory
{

    private static final long serialVersionUID = -4433478558175131280L;

    private MuleMessage message;

    public VariableVariableResolverFactory(ParserConfiguration parserConfiguration,
                                           MuleContext muleContext,
                                           MuleEvent event)
    {
        this.message = event.getMessage();
    }

    @Deprecated
    public VariableVariableResolverFactory(ParserConfiguration parserConfiguration,
                                           MuleContext muleContext,
                                           MuleMessage message)
    {
        this.message = message;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isTarget(String name)
    {
        if (message == null)
        {
            return false;
        }
        return message.getInvocationPropertyNames().contains(name)
               || message.getSessionPropertyNames().contains(name);
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
        else
        {
            return super.getNextFactoryVariableResolver(name);
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
            return message.getMuleContext().getRegistry().lookupObject(name);
        }

        @Override
        public void setValue(Object value)
        {
            throw new UnsupportedOperationException();
        }
    }

}
