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
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.config.i18n.CoreMessages;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.ImmutableElementException;
import org.mvel2.ParserContext;
import org.mvel2.ast.Function;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.BaseVariableResolverFactory;
import org.mvel2.integration.impl.SimpleSTValueResolver;

public abstract class AbstractVariableResolverFactory extends BaseVariableResolverFactory
    implements ExpressionLanguageContext
{

    private static final long serialVersionUID = 909413730991198290L;

    protected ParserContext parserContext;
    protected MuleContext muleContext;
    protected Map<String, String> aliases = new HashMap<String, String>();

    public AbstractVariableResolverFactory(ParserContext parserContext, MuleContext muleContext)
    {
        this.parserContext = parserContext;
        this.muleContext = muleContext;
    }

    @Override
    public boolean isTarget(String name)
    {
        return this.variableResolvers.containsKey(name);
    }

    @Override
    public boolean isResolveable(String name)
    {
        return isTarget(name) || isNextResolveable(name);
    }

    @Override
    public VariableResolver createVariable(String name, Object value)
    {
        return createVariable(name, value, null);
    }

    @Override
    public VariableResolver createVariable(String name, Object value, Class<?> type)
    {
        VariableResolver vr = getVariableResolver(name);
        vr.setValue(value);
        return vr;
    }

    /*
     * (non-Javadoc)
     * @see org.mule.el.mvel.MuleVariableResolverFactory#addVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void addVariable(String name, Object value)
    {
        addResolver(name, new MuleVariableResolver(name, value, value.getClass()));
    }

    /*
     * (non-Javadoc)
     * @see org.mule.el.mvel.MuleVariableResolverFactory#addFinalVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void addFinalVariable(String name, Object value)
    {
        addResolver(name, new MuleFinalVariableResolver(name, value, value.getClass()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getVariable(String name)
    {
        VariableResolver resolver = getVariableResolver(name);
        if (resolver != null)
        {
            return (T) resolver.getValue();
        }
        else
        {
            return null;
        }
    }

    @Override
    public <T> T getVariable(String name, Class<T> type)
    {
        return getVariable(name);
    }

    protected VariableResolver addResolver(String name, VariableResolver vr)
    {
        if (this.variableResolvers == null)
        {
            this.variableResolvers = new HashMap<String, VariableResolver>();
        }
        this.variableResolvers.put(name, vr);
        return vr;
    }

    @Override
    public void addAlias(String alias, String expression)
    {
        aliases.put(alias, expression);
    }

    protected static class MuleVariableResolver extends SimpleSTValueResolver
    {
        private static final long serialVersionUID = -4957789619105599831L;
        private String name;

        public MuleVariableResolver(String name, Object value, Class<?> type)
        {
            super(value, type);
        }

        @Override
        public String getName()
        {
            return name;
        }
    }

    protected static class MuleFinalVariableResolver extends MuleVariableResolver
    {
        private static final long serialVersionUID = -4957789619105599831L;
        private String name;

        public MuleFinalVariableResolver(String name, Object value, Class<?> type)
        {
            super(name, value, type);
        }

        @Override
        public void setValue(Object value)
        {
            throw new ImmutableElementException(CoreMessages.expressionFinalVariableCannotBeAssignedValue(
                name).getMessage());
        }
    }

    @Override
    public void importClass(Class<?> clazz)
    {
        parserContext.addImport(clazz);
    }

    @Override
    public void importClass(String name, Class<?> clazz)
    {
        parserContext.addImport(name, clazz);
    }

    @Override
    public void importStaticMethod(String name, Method method)
    {
        parserContext.addImport(name, method);
    }

    @Override
    public boolean contains(String name)
    {
        return isResolveable(name);
    }

    @Override
    public void declareFunction(String name, ExpressionLanguageFunction function)
    {
        addVariable(name, new MVELFunctionAdaptor(name, function));
    }

    @SuppressWarnings("serial")
    private class MVELFunctionAdaptor extends Function
    {
        private ExpressionLanguageFunction function;

        public MVELFunctionAdaptor(String name, ExpressionLanguageFunction function)
        {
            super(name, new char[]{}, new char[]{}, 0, parserContext);
            this.function = function;
        }

        @Override
        public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms)
        {
            function.validateParams(parms);
            return function.call(parms, AbstractVariableResolverFactory.this);
        }

        @Override
        public void checkArgumentCount(int passing)
        {
            // no-op
        }
    }

    public Map<String, String> getAliases()
    {
        return aliases;
    }
}
