/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

public class TestAttributesResolverWithKeyResolver implements TypeKeysResolver, AttributesTypeResolver<String> {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws ConnectionException {
    return TestMetadataResolverUtils.getKeys(context);
  }

  @Override
  public String getResolverName() {
    return "TestOutputResolverWithKeyResolver";
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }
}
