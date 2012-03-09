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

import java.util.LinkedList;
import java.util.List;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;

public class CompositeVariableResolverFactory extends AbstractVariableResolverFactory
    implements VariableResolverFactory
{

    private static final long serialVersionUID = 5926079137228159391L;

    private List<VariableResolverFactory> resolverFactories = new LinkedList<VariableResolverFactory>();

    public CompositeVariableResolverFactory(ParserContext parserContext,
                                            MuleContext muleContext,
                                            VariableResolverFactory... factories)
    {
        super(parserContext, muleContext);
        for (VariableResolverFactory variableResolverFactory : factories)
        {
            addVariableResolverFactory(variableResolverFactory);
        }
    }

    @Override
    public boolean isResolveable(String name)
    {
        for (VariableResolverFactory factory : resolverFactories)
        {
            if (factory.isResolveable(name))
            {
                return true;
            }
            else if (factory instanceof AbstractVariableResolverFactory
                     && ((AbstractVariableResolverFactory) factory).getAliases().containsKey(name))
            {
                return true;
            };
        }
        return isTarget(name);
    }

    public VariableResolver getVariableResolver(final String name)
    {
        for (VariableResolverFactory factory : resolverFactories)
        {
            if (factory.isResolveable(name))
            {
                return factory.getVariableResolver(name);
            }
            else if (factory instanceof AbstractVariableResolverFactory
                     && ((AbstractVariableResolverFactory) factory).getAliases().containsKey(name))
            {
                return new MuleVariableResolver(name, MVEL.eval(
                    ((AbstractVariableResolverFactory) factory).getAliases().get(name), this), null);
            }
        }
        if (variableResolvers.containsKey(name))
        {
            return variableResolvers.get(name);
        }
        else if (nextFactory != null)
        {
            return nextFactory.getVariableResolver(name);
        }

        throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
    }

    public void addVariableResolverFactory(VariableResolverFactory variableResolverFactory)
    {
        resolverFactories.add(variableResolverFactory);
    }

    @Override
    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory)
    {
        throw new UnsupportedOperationException();
    }

}
