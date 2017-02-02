/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.oauth2.asserter.AuthorizationRequestAsserter;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class AuthorizationCodeExternalUrlTestCase extends AbstractAuthorizationCodeBasicTestCase {

  @Rule
  public SystemProperty externalUrl = new SystemProperty("external.redirect.url", "http://app.cloudhub.com:1234/callback");

  @Override
  protected String getConfigFile() {
    return "authorization-code/authorization-code-externalurl-config.xml";
  }

  @Test
  @Override
  public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception {
    // this inherited test doesn't apply here
  }

  @Test
  public void canDefineExternalUrlThroughXmlAttribute() throws Exception {
    LoggedRequest request = getLoggedRequest();

    AuthorizationRequestAsserter.create(request).assertRedirectUriIs(externalUrl.getValue());
  }
}
