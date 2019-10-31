/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.test.metadata.extension.MetadataConnectionProvider;
import org.mule.test.metadata.extension.MetadataSource;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class TestInputOutputSourceResolverWithKeyResolver
    implements TypeKeysResolver, InputTypeResolver<String>, OutputTypeResolver<String> {

  public static final String STARTED_SOURCE_KEY_MASK = "Source Started = [%s]";
  public static final String STARTED_CONNECTION_PROVIDER_KEY_MASK = "Connection Provider Started = [%s]";
  public static final String TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER = "TestInputOutputSourceResolverWithKeyResolver";

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return ImmutableSet.<MetadataKey>builder()
        .addAll(TestMetadataResolverUtils.getKeys(context))
        .add(newKey(String.format(STARTED_SOURCE_KEY_MASK, MetadataSource.STARTED)).build())
        .add(newKey(String.format(STARTED_CONNECTION_PROVIDER_KEY_MASK, MetadataConnectionProvider.STARTED)).build())
        .build();
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key) throws MetadataResolvingException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException {
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public String getResolverName() {
    return TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER;
  }
}
