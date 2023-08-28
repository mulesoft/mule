/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;
import static org.mule.test.oauth.TestOAuthConnectionProvider.ACCESS_TOKEN_URL;
import static org.mule.test.oauth.TestOAuthConnectionProvider.AUTH_URL;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;

@AuthorizationCode(accessTokenUrl = ACCESS_TOKEN_URL, authorizationUrl = AUTH_URL, credentialsPlacement = BASIC_AUTH_HEADER)
@Alias("with-credentials-placement")
public class TestAuthorizationCodeWithCredentialsPlacementConnectionProvider extends TestOAuthConnectionProvider {

}
