/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.Objects;
import java.util.Set;

@NoImplement
public interface RuntimeExtensionModelProvider {

  ExtensionModel createExtensionModel();

  /**
   * Discovers the extension models provided by the Mule Runtime.
   *
   * @return {@link Set} of the runtime provided {@link ExtensionModel}s.
   */
  static Set<ExtensionModel> discoverRuntimeExtensionModels() {
    return stream(((Iterable<RuntimeExtensionModelProvider>) () -> load(RuntimeExtensionModelProvider.class,
                                                                        RuntimeExtensionModelProvider.class.getClassLoader())
                                                                            .iterator()).spliterator(),
                  false)
                      .map(RuntimeExtensionModelProvider::createExtensionModel)
                      .filter(Objects::nonNull)
                      .collect(toSet());
  }

}
