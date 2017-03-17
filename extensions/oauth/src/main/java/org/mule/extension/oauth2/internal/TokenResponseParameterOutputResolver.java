/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

/**
 * Represents an output resolver for the usage of {@link OAuthOperations#customTokenResponseParam(TokenManagerConfig, String, String)}
 * which will return {@link AnyType} as the elements stored in the {@link ResourceOwnerOAuthContext#tokenResponseParameters} are
 * {@link Object}.
 *
 * @since 4.0
 */
public class TokenResponseParameterOutputResolver implements OutputTypeResolver<String> {

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).anyType().build();
  }

  @Override
  public String getCategoryName() {
    return "TokenResponseParameterOutputResolver";
  }

  @Override
  public String getResolverName() {
    return "TokenResponseParameterOutputAnyTypeResolver";
  }
}
