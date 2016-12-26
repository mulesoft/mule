/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode.functional;

import static org.mule.extension.oauth2.internal.OAuthConstants.CODE_PARAMETER;

import org.mule.extension.oauth2.asserter.OAuthContextFunctionAsserter;

import org.apache.http.client.fluent.Request;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-11276 - Need a way to reuse an http listener declared in the application/domain")
public class AuthorizationCodeLocalCallbackConfigTestCase extends AbstractAuthorizationCodeBasicTestCase {

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-localcallbackref-config.xml";
  }

  @Test
  public void hitRedirectUrlAndGetToken() throws Exception {
    configureWireMockToExpectTokenPathRequestForAuthorizationCodeGrantType();

    Request.Get(localCallbackUrl.getValue() + "?" + CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
        .connectTimeout(REQUEST_TIMEOUT).socketTimeout(REQUEST_TIMEOUT).execute();

    verifyRequestDoneToTokenUrlForAuthorizationCode();

    OAuthContextFunctionAsserter.createFrom(muleContext.getRegistry().get("tokenManagerConfig"))
        .assertAccessTokenIs(ACCESS_TOKEN).assertRefreshTokenIs(REFRESH_TOKEN);
  }
}
