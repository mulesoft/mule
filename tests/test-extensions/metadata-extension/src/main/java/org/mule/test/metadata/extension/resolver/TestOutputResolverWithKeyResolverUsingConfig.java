/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

public class TestOutputResolverWithKeyResolverUsingConfig implements TypeKeysResolver, OutputTypeResolver<String> {

  public static final String TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER = "TestOutputResolverWithKeyResolverWithConfig";

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
    if (!context.getConfig().isPresent()) {
      throw new MetadataResolvingException("Invalid Configuration.", INVALID_CONFIGURATION);
    }

    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }
}
