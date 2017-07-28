/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.builder;

import com.google.common.base.Throwables;
import org.mule.runtime.extension.internal.loader.catalog.model.TypesCatalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Constructs a {@link TypesCatalog} from a set of types.
 * TODO MULE-13214: this class could be removed once MULE-13214 is done
 * @since 4.0
 */
public class TypesCatalogBuilder {

  private final List<TypesResolverBuilder> typesResolverBuilders;

  private URI baseUri;

  public TypesCatalogBuilder(URI baseUri) {
    this.baseUri = baseUri;
    typesResolverBuilders = new ArrayList<>();
  }

  public void addTypesResolver(Consumer<TypesResolverBuilder> typesResolverBuilderConsumer) {
    TypesResolverBuilder typesResolverBuilder = new TypesResolverBuilder(baseUri);
    typesResolverBuilderConsumer.accept(typesResolverBuilder);
    typesResolverBuilders.add(typesResolverBuilder);
  }

  public TypesCatalog build() throws Exception {
    return new TypesCatalog(typesResolverBuilders.stream().map(typesResolverBuilder -> {
      try {
        return typesResolverBuilder.build();
      } catch (Exception e) {
        Throwables.propagate(e);
        return null;
      }
    }).collect(Collectors.toList()));
  }
}
