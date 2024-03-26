/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.metadata;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.metadata.internal.DefaultMetadataResolverFactory;
import org.mule.runtime.metadata.internal.NullMetadataResolverSupplier;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.metadata.AllOfRoutesOutputTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.ChainOutputAttributesPassThroughTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.OneOfRoutesOutputTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.ChainOutputPayloadPassThroughTypeResolver;

/**
 * Utils that allow to set TypeResolvers to an {@link OperationDeclarer}.
 * 
 * @since 4.7.
 */
public class TypeResolverUtils {

  private TypeResolverUtils() {

  }

  /**
   * For a scope declared by {@code operation}, add resolvers that propagate the inner chain's output payload and attributes
   * types.
   *
   * @param operation
   */
  public static void addScopePassThroughOutputTypeResolver(OperationDeclarer operation) {
    operation
        .withModelProperty(new MetadataResolverFactoryModelProperty(() -> new DefaultMetadataResolverFactory(new NullMetadataResolverSupplier(),
                                                                                                             emptyMap(),
                                                                                                             ChainOutputPayloadPassThroughTypeResolver::new,
                                                                                                             ChainOutputAttributesPassThroughTypeResolver::new)));
  }

  /**
   * Sets to the {@code operation} (that declares a Router) a resolver which return type corresponds to the union of the result of
   * each route of the router.
   * 
   * @param operation
   */
  public static void addOneOfRoutesOutputTypeResolver(OperationDeclarer operation) {
    operation
        .withModelProperty(new MetadataResolverFactoryModelProperty(() -> new DefaultMetadataResolverFactory(new NullMetadataResolverSupplier(),
                                                                                                             emptyMap(),
                                                                                                             OneOfRoutesOutputTypeResolver::new,
                                                                                                             new NullMetadataResolverSupplier())));
  }

  /**
   * Sets to the {@code operation} (that declares a Router) a resolver which return type corresponds to an object that includes
   * all the output types of each route of the router.
   * 
   * @param operation
   */
  public static void addAllOfRoutesOutputTypeResolver(OperationDeclarer operation) {
    operation
        .withModelProperty(new MetadataResolverFactoryModelProperty(() -> new DefaultMetadataResolverFactory(new NullMetadataResolverSupplier(),
                                                                                                             emptyMap(),
                                                                                                             AllOfRoutesOutputTypeResolver::new,
                                                                                                             new NullMetadataResolverSupplier())));
  }

}
