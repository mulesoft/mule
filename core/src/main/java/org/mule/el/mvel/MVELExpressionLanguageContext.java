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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.mule.mvel2.ImmutableElementException;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.UnresolveablePropertyException;
import org.mule.mvel2.ast.FunctionInstance;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.BaseVariableResolverFactory;
import org.mule.mvel2.integration.impl.SimpleVariableResolverFactory;

public class MVELExpressionLanguageContext extends BaseVariableResolverFactory
    implements ExpressionLanguageContext
{

    private static final long serialVersionUID = 909413730991198290L;
    public static final String MULE_MESSAGE_INTERNAL_VARIABLE = "_muleMessage";
    public static final String MULE_CONTEXT_INTERNAL_VARIABLE = "_muleContext";

    protected ParserContext parserContext;
    protected MuleContext muleContext;
    protected InternalVariableResolverFactory localFactory;
    protected Map<String, Object> privateVariables = new HashMap<String, Object>();

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
        VariableResolver vr;

        try
        {
            vr = getVariableResolver(name);
        }
        catch (UnresolveablePropertyException e)
        {
            vr = null;
        }

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
        if (privateVariables.containsKey(name))
        {
            return (T) privateVariables.get(name);
        }
        else
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
        addResolver(alias, new MuleAliasVariableResolver(alias, expression, getParentContext()));
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
        addFinalVariable(name, new FunctionInstance(new MVELFunctionAdaptor(name, function, parserContext)));
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

    MVELExpressionLanguageContext getParentContext()
    {
        return this;
    }

    @Override
    public <T> void addPrivateVariable(String name, T value)
    {
        privateVariables.put(name, value);
    };

}
