/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.mel;

import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.registry.MuleRegistry;
import org.mule.module.oauth2.internal.tokenmanager.TokenManagerConfig;

import java.util.Arrays;

/**
 * Function oauthState for accessing OAuth authentication state
 */
public class OAuthContextExpressionLanguageFunction implements ExpressionLanguageFunction
{

    private MuleRegistry registry;

    @Override
    public Object call(final Object[] params, final ExpressionLanguageContext context)
    {
        int numParams = params.length;
        if (numParams < 1)
        {
            throw new IllegalArgumentException("invalid number of arguments, at least a config name must be provided");
        }
        String oauthConfigName = (String) params[0];
        TokenManagerConfig tokenManagerConfig = registry.get(oauthConfigName);
        return tokenManagerConfig.processOauthContextFunctionACall(Arrays.copyOfRange(params, 1, params.length));
    }

    public void setRegistry(MuleRegistry registry)
    {
        this.registry = registry;
    }
}
