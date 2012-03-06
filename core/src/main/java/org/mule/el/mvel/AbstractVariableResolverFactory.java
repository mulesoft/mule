
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.el.mvel.MuleVariableResolverFactory;
import org.mule.config.i18n.CoreMessages;

import java.util.HashMap;

import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.impl.BaseVariableResolverFactory;
import org.mvel2.integration.impl.SimpleSTValueResolver;

public abstract class AbstractVariableResolverFactory extends BaseVariableResolverFactory implements MuleVariableResolverFactory
{

    private static final long serialVersionUID = 909413730991198290L;

    protected ParserContext parserContext;
    protected MuleContext muleContext;

    public AbstractVariableResolverFactory(ParserContext parserContext, MuleContext muleContext)
    {
        this.parserContext = parserContext;
        this.muleContext = muleContext;
    }

    public boolean isTarget(String name)
    {
        return this.variableResolvers.containsKey(name);
    }

    public boolean isResolveable(String name)
    {
        return isTarget(name) || isNextResolveable(name);
    }

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

    public VariableResolver addResolver(String name, VariableResolver vr, Object value)
    {
        if (this.variableResolvers == null)
        {
            this.variableResolvers = new HashMap<String, VariableResolver>();
        }
        this.variableResolvers.put(name, vr);
        vr.setValue(value);
        return vr;
    }

    public VariableResolver addResolver(String name, VariableResolver vr)
    {
        if (this.variableResolvers == null)
        {
            this.variableResolvers = new HashMap<String, VariableResolver>();
        }
        this.variableResolvers.put(name, vr);
        return vr;
    }

    /* (non-Javadoc)
     * @see org.mule.el.mvel.MuleVariableResolverFactory#addVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void addVariable(String name, Object value)
    {
        addResolver(name, new MuleVariableResolver(name, value, value.getClass()));
    }

    /* (non-Javadoc)
     * @see org.mule.el.mvel.MuleVariableResolverFactory#addFinalVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void addFinalVariable(String name, Object value)
    {
        addResolver(name, new MuleFinalVariableResolver(name, value, value.getClass()));
    }

    private static class MuleVariableResolver extends SimpleSTValueResolver
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

    private static class MuleFinalVariableResolver extends MuleVariableResolver
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
            throw new PropertyAccessException(CoreMessages.expressionFinalVariableCannotBeAssignedValue(name)
                .getMessage());
        }
    }

}
