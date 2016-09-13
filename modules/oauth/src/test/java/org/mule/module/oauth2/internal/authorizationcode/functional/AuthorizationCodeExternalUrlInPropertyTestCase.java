/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static org.mule.module.oauth2.internal.authorizationcode.DefaultAuthorizationCodeGrantType.EXTERNAL_REDIRECT_URL_PROPERTY;
import org.mule.module.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeExternalUrlInPropertyTestCase extends AbstractAuthorizationCodeBasicTestCase
{
    @Rule
    public SystemProperty externalUrl = new SystemProperty(EXTERNAL_REDIRECT_URL_PROPERTY, "http://app.cloudhub.com:1234/callback");

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-minimal-config.xml";
    }

    @Test
    @Override
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        // this inherited test doesn't apply here
    }

    @Test
    public void canDefineExternalUrlThroughSystemProperty() throws Exception {
        LoggedRequest request = getLoggedRequest();

        AuthorizationRequestAsserter.create(request)
                .assertRedirectUriIs(externalUrl.getValue());
    }
}
