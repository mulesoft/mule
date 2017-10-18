/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.config.api.dsl.model.ApplicationModel.MODULE_OPERATION_CHAIN;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.dsl.model.ComponentModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Helper class to work with a set of {@link ExtensionModel}s
 * <p/>
 * Contains a cache for searches within the extension models so we avoid processing each extension model twice.
 * 
 * since 4.0
 */
public class ExtensionModelHelper {

  private final DslElementModelFactory modelResolverFactory;
  private Cache<org.mule.runtime.config.api.dsl.model.ComponentModel, Optional<DslElementModel<Object>>> modelByComponentIdentifier =
      CacheBuilder.newBuilder().build();

  /**
   * @param extensionModels the set of {@link ExtensionModel}s to work with. Usually this is the set of models configured within a
   *        mule artifact.
   */
  public ExtensionModelHelper(Set<ExtensionModel> extensionModels) {
    this.modelResolverFactory = DslElementModelFactory.getDefault(DslResolvingContext.getDefault(extensionModels));
  }

  /**
   * Find a {@link DslElementModel} for a given {@link ComponentModel}
   * 
   * @param componentModel the component model from the configuration.
   * @return the {@link DslElementModel} associated with the configuration or an {@link Optional#empty()} if there isn't one.
   */
  public Optional<DslElementModel<Object>> findDslElementModel(org.mule.runtime.config.api.dsl.model.ComponentModel componentModel) {
    try {
      return modelByComponentIdentifier.get(componentModel, () -> {
        ConcurrentMap<org.mule.runtime.config.api.dsl.model.ComponentModel, Optional<DslElementModel<Object>>> cacheAsMap =
            modelByComponentIdentifier.asMap();
        ComponentModel parent = componentModel.getParent();
        while (parent != null && !parent.getIdentifier().equals(MODULE_OPERATION_CHAIN)) {
          if (cacheAsMap.containsKey(parent)) {
            Optional<DslElementModel<Object>> parentElementModel = cacheAsMap.get(parent);
            if (parentElementModel.isPresent()) {
              return findComponentModelWithinDslElementModel(componentModel, parentElementModel.get());
            }
          }
          parent = parent.getParent();
        }
        return modelResolverFactory.create(componentModel.getConfiguration());
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<DslElementModel<Object>>

      findComponentModelWithinDslElementModel(org.mule.runtime.config.api.dsl.model.ComponentModel componentModel,
                                              DslElementModel<Object> dslElementModel) {
    if (dslElementModel.getConfiguration().isPresent()
        && dslElementModel.getConfiguration().get().equals(componentModel.getConfiguration())) {
      return of(dslElementModel);
    }
    List<DslElementModel> containedElements = dslElementModel.getContainedElements();
    for (DslElementModel containedElement : containedElements) {
      Optional<DslElementModel<Object>> foundElementModel =
          findComponentModelWithinDslElementModel(componentModel, containedElement);
      if (foundElementModel.isPresent()) {
        return foundElementModel;
      }
    }
    return empty();
  }
}
