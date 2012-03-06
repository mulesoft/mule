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
import org.mule.el.context.AppContext;
import org.mule.el.context.MuleInstanceContext;
import org.mule.el.context.ServerContext;

import org.mvel2.ParserContext;

/**
 * Add's variable available to the Mule application to the MVEL expression context.
 */
public class AppVariableResolverFactory extends AbstractVariableResolverFactory
{

    private static final long serialVersionUID = -6819292692339684915L;

    public AppVariableResolverFactory(ParserContext parserContext, MuleContext muleContext)
    {
        super(parserContext, muleContext);
        addFinalVariable("server", new ServerContext());
        addFinalVariable("mule", new MuleInstanceContext(muleContext));
        addFinalVariable("app", new AppContext(muleContext));
    }

}
