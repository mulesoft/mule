/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.mel;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;

/**
 * MEL extension for adding OAuth related functions.
 */
public class OAuthExpressionLanguageExtension implements ExpressionLanguageExtension, MuleContextAware, Startable, Initialisable
{

    private OAuthContextExpressionLanguageFunction oauthContextFunction;
    private MuleContext muleContext;

    @Override
    public void start()
    {
        oauthContextFunction.setRegistry(muleContext.getRegistry());
    }

    @Override
    public void configureContext(final ExpressionLanguageContext context)
    {
        context.declareFunction("oauthContext", oauthContextFunction);
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            oauthContextFunction = new OAuthContextExpressionLanguageFunction();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }
}
