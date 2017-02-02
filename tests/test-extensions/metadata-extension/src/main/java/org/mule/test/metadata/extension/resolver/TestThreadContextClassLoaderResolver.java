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
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.util.Reference;

import java.util.Set;

public class TestThreadContextClassLoaderResolver
    implements TypeKeysResolver, InputTypeResolver<String>, OutputTypeResolver<String> {

  private static Reference<ClassLoader> contextClassLoader = new Reference<>();

  public static void reset() {
    contextClassLoader.set(null);
  }

  public static ClassLoader getCurrentState() {
    return contextClassLoader.get();
  }

  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public String getResolverName() {
    return "TestThreadContextClassLoaderResolver";
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    contextClassLoader.set(Thread.currentThread().getContextClassLoader());
    return TestMetadataResolverUtils.getKeys(context);
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key) throws MetadataResolvingException {
    contextClassLoader.set(Thread.currentThread().getContextClassLoader());
    return TestMetadataResolverUtils.getMetadata(key);
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, String key) throws MetadataResolvingException {
    contextClassLoader.set(Thread.currentThread().getContextClassLoader());
    return TestMetadataResolverUtils.getMetadata(key);
  }
}
