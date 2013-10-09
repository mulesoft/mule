/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import org.mule.api.el.VariableAssignmentCallback;

import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.SimpleSTValueResolver;

class MuleVariableResolver<T> extends SimpleSTValueResolver
{
    private static final long serialVersionUID = -4957789619105599831L;
    protected String name;
    protected VariableAssignmentCallback<T> assignmentCallback;

    public MuleVariableResolver(String name, T value, Class<?> type, VariableAssignmentCallback<T> callback)
    {
        super(value, type);
        this.name = name;
        this.assignmentCallback = callback;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @SuppressWarnings("unchecked")
    public T getValue(VariableResolverFactory variableResolverFactory)
    {
        return (T) getValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(Object value)
    {
        if (assignmentCallback != null)
        {
            assignmentCallback.assignValue(name, (T) getValue(), (T) value);
        }
        else
        {
            super.setValue(value);
        }
    }
}
