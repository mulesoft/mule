/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;

import java.util.List;
import java.util.function.Consumer;

/**
 * General contract for a model parser capable of reading additional model properties
 *
 * @since 4.5.0
 */
public interface AdditionalPropertiesModelParser {

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the extension level which are
   * specifically linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   * @deprecated since 1.8 use {@link #getAdditionalModelPropertiesConfigurers()} instead.
   */
  @Deprecated
  List<ModelProperty> getAdditionalModelProperties();

  /**
   * Returns {@link Consumer}s that apply all the {@link ModelProperty model properties} at the extension level which are
   * specifically linked to the type of syntax used to define the extension.
   * 
   * @since 1.8
   */
  default <D extends HasModelProperties> Consumer<D> getAdditionalModelPropertiesConfigurer() {
    return declarer -> getAdditionalModelProperties()
        .forEach(mp -> declarer.withModelProperty(mp));
  }
}
