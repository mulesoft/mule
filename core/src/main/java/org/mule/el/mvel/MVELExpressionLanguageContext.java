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
import java.util.Collections;
import java.util.Map;

import org.mvel2.ImmutableElementException;
import org.mvel2.ParserContext;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.ast.Function;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.BaseVariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.SimpleSTValueResolver;
import org.mvel2.integration.impl.SimpleVariableResolverFactory;

public class MVELExpressionLanguageContext extends BaseVariableResolverFactory
    implements ExpressionLanguageContext
{

    private static final long serialVersionUID = 909413730991198290L;

    protected ParserContext parserContext;
    protected MuleContext muleContext;
    protected InternalVariableResolverFactory localFactory;

    public MVELExpressionLanguageContext(ParserContext parserContext, MuleContext muleContext)
    {
        this.parserContext = parserContext;
        this.muleContext = muleContext;
        this.localFactory = new InternalVariableResolverFactory(Collections.<String, Object> emptyMap());
        this.nextFactory = localFactory;
    }

    public MVELExpressionLanguageContext(MVELExpressionLanguageContext context)
    {
        this.parserContext = context.parserContext;
        this.muleContext = context.muleContext;
        this.localFactory = context.localFactory;
        this.nextFactory = context.nextFactory;
    }

    @Override
    public boolean isTarget(String name)
    {
        return variableResolvers.containsKey(name);
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

    public VariableResolver getVariableResolver(String name)
    {
        if (isResolveable(name))
        {
            if (variableResolvers.containsKey(name))
            {
                return variableResolvers.get(name);
            }
            else
            {
                return nextFactory.getVariableResolver(name);
            }
        }
        throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
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
        addResolver(name, new MuleVariableResolver(name, value, value != null ? value.getClass() : null));
    }

    /*
     * (non-Javadoc)
     * @see org.mule.el.mvel.MuleVariableResolverFactory#addFinalVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void addFinalVariable(String name, Object value)
    {
        addResolver(name, new MuleFinalVariableResolver(name, value, value != null ? value.getClass() : null));
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getVariable(String name, Class<T> type)
    {
        return (T) getVariable(name);
    }

    /*
     * Use an internal VariableResolverFactory in order to use chain resolution while ensuring custom
     * variables from extension aren't given precedence
     */
    protected void addResolver(String name, VariableResolver vr)
    {
        if (this.getClass().equals(MVELExpressionLanguageContext.class))
        {
            localFactory.addResolver(name, vr);
        }
        else
        {
            variableResolvers.put(name, vr);
        }
    }

    @Override
    public void addAlias(String alias, String expression)
    {
        addResolver(alias, new MuleAliasVariableResolver(alias, expression, this));
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

        public Object getValue(VariableResolverFactory variableResolverFactory)
        {
            return getValue();
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

    static protected class MuleAliasVariableResolver extends MuleVariableResolver
    {
        private static final long serialVersionUID = -4957789619105599831L;
        private String expression;
        private MVELExpressionLanguageContext context;
        private MVELExpressionExecutor executor;

        public MuleAliasVariableResolver(String name, String expression, MVELExpressionLanguageContext context)
        {
            super(name, null, null);
            this.expression = expression;
            this.context = context;
            this.executor = new MVELExpressionExecutor(context.parserContext);
        }

        @Override
        public Object getValue()
        {
            return executor.execute(expression, context);
        }

        @Override
        public void setValue(Object value)
        {
            MVELExpressionLanguageContext newContext = new MVELExpressionLanguageContext(context);
            expression = expression + "= ___value";
            newContext.addVariable("___value", value);
            executor.execute(expression, newContext);
        }
    }

    @Override
    public void importClass(Class<?> clazz)
    {
        if (parserContext.hasImport(clazz.getSimpleName()))
        {
            parserContext.addImport(clazz);
        }
    }

    @Override
    public void importClass(String name, Class<?> clazz)
    {
        if (!parserContext.hasImport(name))
        {
            parserContext.addImport(name, clazz);
        }
    }

    @Override
    public void importStaticMethod(String name, Method method)
    {
        if (!parserContext.hasImport(name))
        {
            parserContext.addImport(name, method);
        }
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
            if (factory instanceof ClassImportResolverFactory)
            {
                factory = factory.getNextFactory();
            }
            return function.call(parms, (ExpressionLanguageContext) factory);
        }

        @Override
        public void checkArgumentCount(int passing)
        {
            // no-op
        }
    }

    @Override
    public void appendFactory(VariableResolverFactory resolverFactory)
    {
        if (nextFactory instanceof InternalVariableResolverFactory)
        {
            setNextFactory(resolverFactory);
            resolverFactory.setNextFactory(localFactory);
        }
        else
        {
            VariableResolverFactory vrf = nextFactory;
            while (vrf.getNextFactory() != null
                   && !(vrf.getNextFactory() instanceof InternalVariableResolverFactory))
            {
                vrf = vrf.getNextFactory();
            }
            vrf.setNextFactory(resolverFactory);
            resolverFactory.setNextFactory(localFactory);
        }
    }

    @SuppressWarnings("serial")
    class InternalVariableResolverFactory extends SimpleVariableResolverFactory
    {
        public InternalVariableResolverFactory(Map<String, Object> variables)
        {
            super(variables);
        }

        public void addResolver(String name, VariableResolver resolver)
        {
            variableResolvers.put(name, resolver);
        }
    }
}
