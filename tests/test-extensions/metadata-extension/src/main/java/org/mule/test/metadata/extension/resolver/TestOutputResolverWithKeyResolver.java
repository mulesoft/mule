/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Set;

public class TestOutputResolverWithKeyResolver implements TypeKeysResolver, OutputTypeResolver<String> {

  public static final String TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER = "TestOutputResolverWithKeyResolver";

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws ConnectionException {
    return TestMetadataResolverUtils.getKeys(context);
  }

  @Override
  public String getResolverName() {
    return TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER;
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }
}
