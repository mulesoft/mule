/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode.functional;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.http.client.fluent.Request.Get;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.module.http.internal.HttpParser.appendQueryParam;
import static org.mule.module.oauth2.internal.OAuthConstants.CODE_PARAMETER;
import static org.mule.module.oauth2.internal.OAuthConstants.STATE_PARAMETER;
import static org.mule.module.oauth2.internal.authorizationcode.AutoAuthorizationCodeTokenRequestHandler.NO_AUTHORIZATION_CODE_STATUS;
import static org.mule.module.oauth2.internal.authorizationcode.AutoAuthorizationCodeTokenRequestHandler.TOKEN_NOT_FOUND_STATUS;
import static org.mule.module.oauth2.internal.authorizationcode.AutoAuthorizationCodeTokenRequestHandler.TOKEN_URL_CALL_FAILED_STATUS;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import org.mule.module.oauth2.internal.StateEncoder;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeAuthorizationFailureTestCase extends AbstractAuthorizationCodeBasicTestCase
{

    private static final String EXPECTED_STATUS_CODE_SYSTEM_PROPERTY = "expectedStatusCode";

    @Rule
    public DynamicPort onCompleteUrlPort = new DynamicPort("onCompleteUrlPort");

    @Override
    protected String getConfigFile()
    {
        return "authorization-code/authorization-code-failure-scenarios-config.xml";
    }

    @Test
    public void urlRedirectHandlerDoNotRetrieveAuthorizationCode() throws Exception
    {
        Response response = Get(redirectUrl.getValue())
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void urlRedirectHandlerDoNotRetrieveAuthorizationCodeWithOnCompleteRedirect() throws Exception
    {
        testWithSystemProperty(EXPECTED_STATUS_CODE_SYSTEM_PROPERTY, valueOf(NO_AUTHORIZATION_CODE_STATUS), new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                Response response = Get(getRedirectUrlWithOnCompleteUrlQueryParam())
                        .connectTimeout(REQUEST_TIMEOUT)
                        .socketTimeout(REQUEST_TIMEOUT)
                        .execute();
                response.returnResponse();

                FlowAssert.verify();
            }
        });
    }

    @Test
    public void callToTokenUrlFails() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeAndFail();

        verifyCallToRedirectUrlFails();
    }

    @Test
    public void callToTokenUrlFailsWithOnCompleteRedirect() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeAndFail();

        testWithSystemProperty(EXPECTED_STATUS_CODE_SYSTEM_PROPERTY, valueOf(TOKEN_URL_CALL_FAILED_STATUS), new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams())
                        .connectTimeout(REQUEST_TIMEOUT)
                        .socketTimeout(REQUEST_TIMEOUT)
                        .execute();

                FlowAssert.verify();
            }
        });

        verifyCallToRedirectUrlFails();
    }

    @Test
    public void callToTokenUrlSuccessButNoAccessTokenRetrieved() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantTypeWithBody(EMPTY);

        verifyCallToRedirectUrlFails();
    }

    @Test
    public void callToTokenUrlSuccessButNoRefreshTokenRetrieved() throws Exception
    {
        configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType(ACCESS_TOKEN, null);

        testWithSystemProperty(EXPECTED_STATUS_CODE_SYSTEM_PROPERTY, valueOf(TOKEN_NOT_FOUND_STATUS), new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                Get(getRedirectUrlWithOnCompleteUrlAndCodeQueryParams())
                        .connectTimeout(REQUEST_TIMEOUT)
                        .socketTimeout(REQUEST_TIMEOUT)
                        .execute();

                FlowAssert.verify();
            }
        });

    }

    private void verifyCallToRedirectUrlFails() throws IOException
    {
        Response response = Get(format(redirectUrl.getValue() + "%s%s=%s", "?", CODE_PARAMETER, AUTHENTICATION_CODE))
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();
        HttpResponse httpResponse = response.returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    private String getRedirectUrlWithOnCompleteUrlQueryParam()
    {
        StateEncoder stateEncoder = new StateEncoder(null);
        stateEncoder.encodeOnCompleteRedirectToInState(format("http://localhost:%s/afterLogin", onCompleteUrlPort.getNumber()));
        return appendQueryParam(redirectUrl.getValue(), STATE_PARAMETER, stateEncoder.getEncodedState());
    }

    private String getRedirectUrlWithOnCompleteUrlAndCodeQueryParams()
    {
        return appendQueryParam(getRedirectUrlWithOnCompleteUrlQueryParam(), CODE_PARAMETER, AUTHENTICATION_CODE);
    }

}
