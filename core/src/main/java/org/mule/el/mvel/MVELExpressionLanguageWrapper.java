/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.expression.DefaultExpressionManager;

/**
 * Wraps a {@link MVELExpressionLanguage} to take care of injecting new instances in the
 * muleContext's {@link DefaultExpressionManager}
 */
public final class MVELExpressionLanguageWrapper extends MVELExpressionLanguage
{

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
