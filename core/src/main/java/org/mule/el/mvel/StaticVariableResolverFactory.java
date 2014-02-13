/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.el.context.AppContext;
import org.mule.el.context.MuleInstanceContext;
import org.mule.el.context.ServerContext;
import org.mule.el.function.RegexExpressionLanguageFuntion;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.ast.FunctionInstance;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.mvel2.integration.impl.ImmutableDefaultFactory;
import org.mule.mvel2.integration.impl.SimpleValueResolver;

class StaticVariableResolverFactory extends ImmutableDefaultFactory
{

    private static final long serialVersionUID = -1448079302094120410L;

    private static final String SERVER = "server";
    private static final String MULE = "mule";
    private static final String APP = "app";
    private static final String REGEX = "regex";
    private static final String DATE_TIME = "dateTime";

    private MuleContext muleContext;
    private FunctionInstance regexFunction;
    private FunctionInstance dateTimeFunction;

    public StaticVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        regexFunction = new FunctionInstance(new MVELFunctionAdaptor(REGEX,
            new RegexExpressionLanguageFuntion(), new ParserContext(parserConfiguration)));
        dateTimeFunction = new FunctionInstance(new MVELFunctionAdaptor(REGEX,
            new RegexExpressionLanguageFuntion(), new ParserContext(parserConfiguration)));

    }

    @Override
    public VariableResolver getVariableResolver(String name)
    {
        if (SERVER.equals(name))
        {
            return new SimpleValueResolver(new ServerContext());
        }
        else if (MULE.equals(name))
        {
            return new SimpleValueResolver(new MuleInstanceContext(muleContext));
        }
        else if (APP.equals(name))
        {
            return new SimpleValueResolver(new AppContext(muleContext));
        }
        else if (REGEX.equals(name))
        {
            return new SimpleValueResolver(regexFunction);
        }
        else if (DATE_TIME.equals(name))
        {
            return new SimpleValueResolver(dateTimeFunction);
        }
        else if (MVELExpressionLanguageContext.MULE_CONTEXT_INTERNAL_VARIABLE.equals(name))
        {
            return new SimpleValueResolver(muleContext);
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isTarget(String name)
    {
        return SERVER.equals(name) || MULE.equals(name) || APP.equals(name) || REGEX.equals(name)
               || DATE_TIME.equals(name)
               || MVELExpressionLanguageContext.MULE_CONTEXT_INTERNAL_VARIABLE.equals(name);
    }

    @Override
    public boolean isResolveable(String name)
    {
        return isTarget(name);
    }

}
