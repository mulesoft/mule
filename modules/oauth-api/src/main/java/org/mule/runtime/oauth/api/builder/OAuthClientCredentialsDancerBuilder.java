/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.builder;

import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;

/**
 * Builder that allows to configure the attributes for the client credentials grant type.
 *
 * @since 4.0
 */
public interface OAuthClientCredentialsDancerBuilder extends OAuthDancerBuilder<ClientCredentialsOAuthDancer> {

  /**
   * @param encodeClientCredentialsInBody If @{code true}, the client id and client secret will be sent in the request body.
   *        Otherwise, they will be sent as basic authentication.
   * 
   * @return this builder
   */
  OAuthClientCredentialsDancerBuilder encodeClientCredentialsInBody(boolean encodeClientCredentialsInBody);

}
