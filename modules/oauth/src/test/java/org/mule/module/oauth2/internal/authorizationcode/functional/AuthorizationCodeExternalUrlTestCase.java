/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.oauth2.internal.authorizationcode.DefaultAuthorizationCodeGrantType.EXTERNAL_REDIRECT_URL_PROPERTY;
import org.mule.module.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.List;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeExternalUrlTestCase extends AbstractAuthorizationCodeBasicTestCase
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
        wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

        Request.Get(localAuthorizationUrl.getValue()).connectTimeout(RECEIVE_TIMEOUT).socketTimeout(RECEIVE_TIMEOUT).execute();

        final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
        assertThat(requests.size(), is(1));

        AuthorizationRequestAsserter.create((requests.get(0)))
                .assertRedirectUriIs(externalUrl.getValue());
    }

}
