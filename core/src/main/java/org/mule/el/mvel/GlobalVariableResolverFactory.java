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
import org.mule.api.el.ExpressionLanguageExtension;

import java.util.Map.Entry;

import org.mvel2.ParserContext;
import org.mvel2.ast.Function;

public class GlobalVariableResolverFactory extends MVELExpressionLanguageContext
{
    private MVELExpressionLanguageContext parent;

    private static final long serialVersionUID = -6819292692339684915L;

    public GlobalVariableResolverFactory(MVELExpressionLanguage el,
                                         MVELExpressionLanguageContext parent,
                                         ParserContext parserContext,
                                         MuleContext muleContext)
    {
        super(parserContext, muleContext);
        this.parent = parent;
        for (ExpressionLanguageExtension extension : muleContext.getRegistry().lookupObjectsForLifecycle(
            ExpressionLanguageExtension.class))
        {
            extension.configureContext(parent);
        }
        for (Entry<String, Class<?>> function : el.imports.entrySet())
        {
            importClass(function.getKey(), function.getValue());
        }
        for (Entry<String, String> alias : el.aliases.entrySet())
        {
            addAlias(alias.getKey(), alias.getValue());
        }
        for (Entry<String, Function> function : el.globalFunctions.entrySet())
        {
            addFinalVariable(function.getKey(), function.getValue());
        }
    }

    @Override
    MVELExpressionLanguageContext getParentContext()
    {
        return parent;
    }
}
