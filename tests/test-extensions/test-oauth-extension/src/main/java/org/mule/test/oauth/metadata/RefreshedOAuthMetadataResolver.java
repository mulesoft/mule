/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.metadata;

import static java.util.Arrays.asList;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.test.oauth.TestOAuthConnection;

import java.util.HashSet;
import java.util.Set;

public class RefreshedOAuthMetadataResolver implements OutputTypeResolver<String>, AttributesTypeResolver<String>,
    InputTypeResolver<String>, TypeKeysResolver, QueryEntityResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    checkIfTokenIsRefreshed(context);
    return new HashSet(asList(newKey("anyKey").build()));
  }

  @Override
  public String getResolverName() {
    return "Metadata resolver";
  }

  @Override
  public Set<MetadataKey> getEntityKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    checkIfTokenIsRefreshed(context);
    return new HashSet(asList(newKey("anyKey").build()));
  }

  @Override
  public MetadataType getEntityMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    checkIfTokenIsRefreshed(context);
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    checkIfTokenIsRefreshed(context);
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    checkIfTokenIsRefreshed(context);
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException {
    checkIfTokenIsRefreshed(context);
    return BaseTypeBuilder.create(JAVA).stringType().build();
  }

  private void checkIfTokenIsRefreshed(MetadataContext context) throws ConnectionException {
    TestOAuthConnection testOAuthConnection = (TestOAuthConnection) context.getConnection().get();
    if (!testOAuthConnection.getState().getState().getAccessToken().contains("refreshed")) {
      throw new AccessTokenExpiredException();
    }
  }

  @Override
  public String getCategoryName() {
    return "OAuth Category";
  }

}
