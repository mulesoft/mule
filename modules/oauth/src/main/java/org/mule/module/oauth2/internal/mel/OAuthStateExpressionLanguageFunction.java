/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.mel;

import org.mule.api.MuleRuntimeException;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.oauth2.internal.state.ConfigOAuthState;
import org.mule.module.oauth2.internal.state.OAuthStateRegistry;
import org.mule.module.oauth2.internal.state.UserOAuthState;

/**
 * Function oauthState for accessing OAuth authentication state
 */
public class OAuthStateExpressionLanguageFunction implements ExpressionLanguageFunction
{

    private org.mule.module.oauth2.internal.state.OAuthStateRegistry oauthStateRegistry;

    @Override
    public Object call(final Object[] params, final ExpressionLanguageContext context)
    {
        int numParams = params.length;
        if (numParams < 1 || numParams > 2)
        {
            throw new IllegalArgumentException("invalid number of arguments");
        }

        String oauthConfigName = (String) params[0];
        String userId = UserOAuthState.DEFAULT_USER_ID;
        if (params.length == 2)
        {
            userId = (String) params[1];
        }

        final ConfigOAuthState stateForConfig = oauthStateRegistry.getStateForConfig(oauthConfigName);
        if (stateForConfig == null)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("No state holder for config %s found. Verify that an oauth config with name %s exists", oauthConfigName, oauthConfigName));
        }
        return stateForConfig.getStateForUser(userId);
    }

    public void setOAuthStateRegistry(OAuthStateRegistry OAuthStateRegistry)
    {
        this.oauthStateRegistry = OAuthStateRegistry;
    }
}
