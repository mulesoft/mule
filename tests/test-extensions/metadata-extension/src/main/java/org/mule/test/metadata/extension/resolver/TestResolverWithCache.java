/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.APPLICATION_JAVA_MIME_TYPE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public class TestResolverWithCache
    implements InputTypeResolver<String>, OutputTypeResolver<String>, TypeKeysResolver {

  public static final String MISSING_ELEMENT_ERROR_MESSAGE =
      "Missing element in the cache. There was no element in the cache for the key: " + BRAND;
  public static final int AGE_VALUE = 16;
  public static final String NAME_VALUE = "Juan";
  public static final String BRAND_VALUE = "Nikdidas";

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public String getResolverName() {
    return "TestResolverWithCache";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    MetadataCache cache = context.getCache();
    Optional<? extends Serializable> element = cache.get(BRAND);
    if (!element.isPresent()) {
      throw new MetadataResolvingException(MISSING_ELEMENT_ERROR_MESSAGE, FailureCode.RESOURCE_UNAVAILABLE);
    }

    return buildMetadataType((String) element.get());
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return buildMetadataType(context.getCache().computeIfAbsent(BRAND, (k) -> BRAND_VALUE));
  }

  private MetadataType buildMetadataType(String model) {
    return BaseTypeBuilder.create(new MetadataFormat(model, model, APPLICATION_JAVA_MIME_TYPE)).objectType().build();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    context.getCache().put(AGE, AGE_VALUE);
    context.getCache().put(NAME, NAME_VALUE);
    return TestMetadataResolverUtils.getKeys(context);
  }
}
