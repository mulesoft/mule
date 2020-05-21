/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.core.api.connection.util.ConnectionProviderUtils.unwrapProviderWrapper;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BASIC_AUTH_HEADER;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.BODY;
import static org.mule.runtime.extension.api.security.CredentialsPlacement.QUERY_PARAMS;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.security.CredentialsPlacement;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeConfig;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.ImmutableAuthorizationCodeState;
import org.mule.runtime.oauth.api.builder.ClientCredentialsLocation;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

/**
 * Utility methods for the OAuth support on the SDK
 *
 * @since 4.0
 */
public final class ExtensionsOAuthUtils {

  public static AuthorizationCodeState toAuthorizationCodeState(AuthorizationCodeConfig config,
                                                                ResourceOwnerOAuthContext context) {
    return new ImmutableAuthorizationCodeState(context.getAccessToken(),
                                               context.getRefreshToken(),
                                               context.getResourceOwnerId(),
                                               context.getExpiresIn(),
                                               context.getState(),
                                               config.getAuthorizationUrl(),
                                               config.getAccessTokenUrl(),
                                               config.getCallbackConfig().getExternalCallbackUrl(),
                                               config.getConsumerKey(),
                                               config.getConsumerSecret());
  }

  public static ClientCredentialsLocation toCredentialsLocation(CredentialsPlacement placement) {
    if (placement == BASIC_AUTH_HEADER) {
      return ClientCredentialsLocation.BASIC_AUTH_HEADER;
    } else if (placement == QUERY_PARAMS) {
      return ClientCredentialsLocation.QUERY_PARAMS;
    } else if (placement == BODY) {
      return ClientCredentialsLocation.BODY;
    } else {
      throw new IllegalArgumentException("Unsupported CredentialsPlacement type " + placement.name());
    }
  }

  public static OAuthConnectionProviderWrapper getOAuthConnectionProvider(ExecutionContextAdapter operationContext) {
    ConfigurationInstance config = ((ConfigurationInstance) operationContext.getConfiguration().get());
    ConnectionProvider provider =
        unwrapProviderWrapper(config.getConnectionProvider().get(), OAuthConnectionProviderWrapper.class);
    return provider instanceof OAuthConnectionProviderWrapper ? (OAuthConnectionProviderWrapper) provider : null;
  }

  private ExtensionsOAuthUtils() {}
}
