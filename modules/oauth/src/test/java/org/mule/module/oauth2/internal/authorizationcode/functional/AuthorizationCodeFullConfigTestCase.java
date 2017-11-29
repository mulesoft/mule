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
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.transport.NullPayload;
import org.mule.transport.ssl.DefaultTlsContextFactory;
import org.mule.transport.ssl.api.TlsContextFactory;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AuthorizationCodeFullConfigTestCase extends AbstractAuthorizationCodeFullConfigTestCase
{

    private String configFile;

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    public AuthorizationCodeFullConfigTestCase(String configFile)
    {
        this.configFile = configFile;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[] {"authorization-code/authorization-code-full-config-tls-global.xml"},
                             new Object[] {"authorization-code/authorization-code-full-config-tls-nested.xml"});
    }

    @Test
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

        HttpRequestOptions options =  newOptions()
                .enableFollowsRedirect()
                .tlsContextFactory(createClientTlsContextFactory())
                .build();

        muleContext.getClient().send(localAuthorizationUrl.getValue(), getTestMuleMessage(NullPayload.getInstance()), options);

        final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
        assertThat(requests.size(), is(1));

        AuthorizationRequestAsserter.create((requests.get(0)))
                .assertMethodIsGet()
                .assertClientIdIs(clientId.getValue())
                .assertRedirectUriIs(redirectUrl.getValue())
                .assertScopeIs(scopes.getValue())
                .assertStateIs(state.getValue())
                .assertContainsCustomParameter(authenticationRequestParam1.getValue(), authenticationRequestValue1.getValue())
                .assertContainsCustomParameter(authenticationRequestParam2.getValue(), authenticationRequestValue2.getValue())
                .assertResponseTypeIsCode();
    }

    protected TlsContextFactory createClientTlsContextFactory() throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setTrustStorePath("ssltest-cacerts.jks");
        tlsContextFactory.setTrustStorePassword("changeit");
        tlsContextFactory.setKeyStorePath("ssltest-keystore.jks");
        tlsContextFactory.setKeyStorePassword("changeit");
        tlsContextFactory.setKeyManagerPassword("changeit");

        return tlsContextFactory;
    }

    @Override
    protected String getProtocol()
    {
        return HTTPS.getScheme();
    }
}
