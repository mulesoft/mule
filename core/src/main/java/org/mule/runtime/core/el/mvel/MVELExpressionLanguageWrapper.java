/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.expression.DefaultExpressionManager;

import javax.inject.Inject;

/**
 * Wraps a {@link MVELExpressionLanguage} to take care of injecting new instances in the
 * muleContext's {@link DefaultExpressionManager}
 */
public final class MVELExpressionLanguageWrapper extends MVELExpressionLanguage
{
    @Inject
    public MVELExpressionLanguageWrapper(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (muleContext.getExpressionManager() instanceof DefaultExpressionManager)
        {
            ((DefaultExpressionManager) muleContext.getExpressionManager()).setExpressionLanguage(this);
        }
        super.initialise();
    }
}
