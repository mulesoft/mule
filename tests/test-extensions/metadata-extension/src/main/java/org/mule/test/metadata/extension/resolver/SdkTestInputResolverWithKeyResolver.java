/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.MetadataKey;
import org.mule.sdk.api.metadata.resolving.InputTypeResolver;
import org.mule.sdk.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

public class SdkTestInputResolverWithKeyResolver implements TypeKeysResolver, InputTypeResolver<String> {

  public static final String SDK_TEST_INPUT_RESOLVER_WITH_KEY_RESOLVER = "SdkTestInputResolverWithKeyResolver";

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getSdkKeys(context);
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key) throws MetadataResolvingException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public String getResolverName() {
    return SDK_TEST_INPUT_RESOLVER_WITH_KEY_RESOLVER;
  }
}
