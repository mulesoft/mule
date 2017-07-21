/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
