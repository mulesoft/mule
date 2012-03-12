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

import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolver;

class RegistryVariableResolverFactory extends MVELExpressionLanguageContext
{

    private static final long serialVersionUID = -4433478558175131280L;

    public RegistryVariableResolverFactory(ParserContext parserContext, MuleContext muleContext)
    {
        super(parserContext, muleContext);
    }

    public boolean isTarget(String name)
    {
        return muleContext.getRegistry().lookupObject(name) != null;
    }

    @Override
    public VariableResolver getVariableResolver(String name)
    {
        if (isResolveable(name))
        {
            if (isTarget(name))
            {
                return new RegistryVariableVariableResolver(name);
            }
            else if (nextFactory != null)
            {
                return nextFactory.getVariableResolver(name);
            }
        }
        return null;
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
            Object value = muleContext.getRegistry().lookupObject(name);
            if (value != null)
            {
                return value;
            }
            else
            {
                return null;
            }
        }

        @Override
        public void setValue(Object value)
        {
            throw new UnsupportedOperationException();
        }
    }

}
