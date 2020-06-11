/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.query;

import static org.mule.test.metadata.extension.query.NativeQueryOutputResolver.QUERY_RESOLVER_CATEGORY;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils;

import java.util.Set;

public class QueryExtraResolver implements InputTypeResolver<String>, TypeKeysResolver {

  @Override
  public String getResolverName() {
    return "QueryExtraMetadataResolver";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getKeys(context);
  }

  @Override
  public String getCategoryName() {
    return QUERY_RESOLVER_CATEGORY;
  }
}
