/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.el.VariableAssignmentCallback;
import org.mule.config.i18n.CoreMessages;
import org.mule.mvel2.ImmutableElementException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.ast.FunctionInstance;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.BaseVariableResolverFactory;

import java.lang.reflect.Method;

public class MVELExpressionLanguageContext extends BaseVariableResolverFactory
    implements ExpressionLanguageContext
{

    private static final long serialVersionUID = 909413730991198290L;
    public static final String MULE_MESSAGE_INTERNAL_VARIABLE = "_muleMessage";
    public static final String MULE_EVENT_INTERNAL_VARIABLE = "_muleEvent";
    public static final String MULE_CONTEXT_INTERNAL_VARIABLE = "_muleContext";

    protected ParserConfiguration parserConfiguration;
    protected MuleContext muleContext;

    public MVELExpressionLanguageContext(ParserConfiguration parserConfiguration, MuleContext muleContext)
    {
        this.parserConfiguration = parserConfiguration;
        this.muleContext = muleContext;
    }

    public MVELExpressionLanguageContext(MVELExpressionLanguageContext context)
    {
        this.parserConfiguration = context.parserConfiguration;
        this.muleContext = context.muleContext;
        this.nextFactory = context.nextFactory;
        this.variableResolvers = context.variableResolvers;
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
        VariableResolver variableResolver = variableResolvers.get(name);
        if (variableResolver == null)
        {
            variableResolver = getNextVariableResolver(name);
        }
        // In order to allow aliases to use message context without requiring the creating of a
        // GlobalVariableResolver for each expression evaluation, we create a new resolver on the fly with
        // current context instead.
        if (variableResolver instanceof MuleAliasVariableResolver)
        {
            variableResolver = new MuleAliasVariableResolver((MuleAliasVariableResolver) variableResolver,
                this);
        }
        return variableResolver;
    }

    protected VariableResolver getNextVariableResolver(String name)
    {
        if (nextFactory != null)
        {
            return nextFactory.getVariableResolver(name);
        }
        return null;
    }

    @Override
    public VariableResolver createVariable(String name, Object value, Class<?> type)
    {
        VariableResolver vr = getVariableResolver(name);

        if (vr != null)
        {
            vr.setValue(value);
        }
        else
        {
            addResolver(name, vr = new MuleVariableResolver(name, value, type, null));
        }
        return vr;
    }

    /*
     * (non-Javadoc)
     * @see org.mule.el.mvel.MuleVariableResolverFactory#addVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public <T> void addVariable(String name, T value)
    {
        addResolver(name, new MuleVariableResolver<T>(name, value, value != null ? value.getClass() : null,
            null));
    }

    @Override
    public <T> void addVariable(String name, T value, VariableAssignmentCallback<T> assignmentCallback)
    {
        addResolver(name, new MuleVariableResolver<T>(name, value, value != null ? value.getClass() : null,
            assignmentCallback));
    }

    @Override
    public <T> void addFinalVariable(String name, T value)
    {
        addVariable(name, value, new VariableAssignmentCallback<T>()
        {
            @Override
            public void assignValue(String name, T value, T newValue)
            {
                throw new ImmutableElementException(
                    CoreMessages.expressionFinalVariableCannotBeAssignedValue(name).getMessage());
            }
        });
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
        variableResolvers.put(name, vr);
    }

    @Override
    public void addAlias(String alias, String expression)
    {
        addResolver(alias, new MuleAliasVariableResolver(alias, expression, this, parserConfiguration));
    }

    @Override
    public void importClass(Class<?> clazz)
    {
        if (parserConfiguration.hasImport(clazz.getSimpleName()))
        {
            parserConfiguration.addImport(clazz);
        }
    }

    @Override
    public void importClass(String name, Class<?> clazz)
    {
        if (!parserConfiguration.hasImport(name))
        {
            parserConfiguration.addImport(name, clazz);
        }
    }

    @Override
    public void importStaticMethod(String name, Method method)
    {
        if (!parserConfiguration.hasImport(name))
        {
            parserConfiguration.addImport(name, method);
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
        addFinalVariable(name, new FunctionInstance(new MVELFunctionAdaptor(name, function,
            new ParserContext(parserConfiguration))));
    }

    @Override
    public void appendFactory(VariableResolverFactory resolverFactory)
    {
        if (nextFactory == null)
        {
            setNextFactory(resolverFactory);
        }
        else
        {
            VariableResolverFactory vrf = nextFactory;
            while (vrf.getNextFactory() != null)
            {
                vrf = vrf.getNextFactory();
            }
            vrf.setNextFactory(resolverFactory);
        }
    }

    public void insertFactory(VariableResolverFactory resolverFactory)
    {
        if (nextFactory == null)
        {
            nextFactory = resolverFactory;
        }
        else
        {
            VariableResolverFactory currentNext = nextFactory;
            nextFactory = resolverFactory;
            resolverFactory.setNextFactory(currentNext);
        }
    }

    @Override
    public <T> void addPrivateVariable(String name, T value)
    {
        addFinalVariable(name, value);
    }

}
