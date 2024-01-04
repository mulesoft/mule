/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider.enricher;

import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Allows Mule modules to contribute enrichers for Runtime Extension Models.
 */
@NoImplement
public interface RuntimeExtensionModelEnrichersProvider {

  /**
   * Discovers the extension models provided by the Mule Runtime.
   *
   * @return {@link Set} of the runtime provided {@link ExtensionModel}s.
   */
  static Set<RuntimeExtensionModelEnrichersProvider> discoverRuntimeExtensionModelEnrichersProvider() {
    return stream(((Iterable<RuntimeExtensionModelEnrichersProvider>) () -> load(RuntimeExtensionModelEnrichersProvider.class,
                                                                                 RuntimeExtensionModelEnrichersProvider.class
                                                                                     .getClassLoader())
                                                                                         .iterator()).spliterator(),
                  false)
                      .filter(Objects::nonNull)
                      .collect(toSet());
  }

  /**
   * 
   * 
   * @return {@link DeclarationEnricher}s to be applied on core extension models only.
   */
  public List<DeclarationEnricher> getEnrichers();
}
