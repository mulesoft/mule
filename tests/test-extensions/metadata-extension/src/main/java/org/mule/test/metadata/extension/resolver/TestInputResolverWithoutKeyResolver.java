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
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

public class TestInputResolverWithoutKeyResolver implements InputTypeResolver<String> {

  public static final String TEST_INPUT_RESOLVER_WITHOUT_KEY_RESOLVER = "TestInputResolverWithoutKeyResolver";

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public String getResolverName() {
    return TEST_INPUT_RESOLVER_WITHOUT_KEY_RESOLVER;
  }
}
