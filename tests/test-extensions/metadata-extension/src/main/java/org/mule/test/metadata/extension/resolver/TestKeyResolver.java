/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

public class TestKeyResolver implements TypeKeysResolver {

  @Override
  public String getCategoryName() {
    return "TestResolvers";
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context)
      throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getKeys(context);
  }
}
