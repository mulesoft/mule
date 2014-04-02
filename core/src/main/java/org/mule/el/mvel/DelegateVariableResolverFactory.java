/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;

import java.util.Set;

class DelegateVariableResolverFactory implements VariableResolverFactory
{

    private static final long serialVersionUID = 1625380094897107954L;
    protected VariableResolverFactory delegate;
    protected VariableResolverFactory next;

    public DelegateVariableResolverFactory(VariableResolverFactory delegate)
    {
        this.delegate = delegate;
    }

    public DelegateVariableResolverFactory(VariableResolverFactory delegate, VariableResolverFactory next)
    {
        this.delegate = delegate;
        this.next = next;
    }

    public VariableResolver createVariable(String name, Object value)
    {
        return delegate.createVariable(name, value);
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value)
    {
        return delegate.createIndexedVariable(index, name, value);
    }

    public VariableResolver createVariable(String name, Object value, Class<?> type)
    {
        return delegate.createVariable(name, value, type);
    }

    public VariableResolver createIndexedVariable(int index, String name, Object value, Class<?> typee)
    {
        return delegate.createIndexedVariable(index, name, value, typee);
    }

    public VariableResolver setIndexedVariableResolver(int index, VariableResolver variableResolver)
    {
        return delegate.setIndexedVariableResolver(index, variableResolver);
    }

    public VariableResolverFactory getNextFactory()
    {
        return next;
    }

    public VariableResolverFactory setNextFactory(VariableResolverFactory resolverFactory)
    {
        return next = resolverFactory;
    }

    public VariableResolver getVariableResolver(String name)
    {
        VariableResolver vr = delegate.getVariableResolver(name);
        if (vr == null && next != null)
        {
            vr = next.getVariableResolver(name);
        }
        return vr;
    }

    public VariableResolver getIndexedVariableResolver(int index)
    {
        return delegate.getIndexedVariableResolver(index);
    }

    public boolean isTarget(String name)
    {
        return delegate.isTarget(name);
    }

    public boolean isResolveable(String name)
    {
        return delegate.isResolveable(name) || (next != null && next.isResolveable(name));
    }

    public Set<String> getKnownVariables()
    {
        return delegate.getKnownVariables();
    }

    public int variableIndexOf(String name)
    {
        return delegate.variableIndexOf(name);
    }

    public boolean isIndexedFactory()
    {
        return delegate.isIndexedFactory();
    }

    public boolean tiltFlag()
    {
        return delegate.tiltFlag();
    }

    public void setTiltFlag(boolean tilt)
    {
        delegate.setTiltFlag(tilt);
    }

}
