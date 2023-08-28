/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.test.classloading.api.ClassLoadingHelper.addClassLoader;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class CLKeysResolver implements TypeKeysResolver, OutputTypeResolver {

  public static final String GET_METADATA = "Metadata";

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    addClassLoader(GET_METADATA);
    return ImmutableSet.of(MetadataKeyBuilder.newKey("VALUE").build());
  }

  @Override
  public String getResolverName() {
    return "ClassLoadingResolver";
  }

  @Override
  public String getCategoryName() {
    return "ClassLoadingCategory";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key) throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).anyType().build();
  }
}
