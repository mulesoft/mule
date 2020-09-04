/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.sdk.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.test.oauth.TestOAuthConnection;

public class OAuthMetadataResolver implements OutputTypeResolver {

  @Override
  public String getResolverName() {
    return "Metadata resolver";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    TestOAuthConnection testOAuthConnection = (TestOAuthConnection) context.getConnection().get();
    if (!testOAuthConnection.getState().getState().getAccessToken().contains("refreshed")) {
      throw new AccessTokenExpiredException();
    }
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public String getCategoryName() {
    return "OAuth Category";
  }
}
