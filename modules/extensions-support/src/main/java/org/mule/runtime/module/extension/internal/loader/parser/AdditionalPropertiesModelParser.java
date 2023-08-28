/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;

import java.util.List;

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
   */
  List<ModelProperty> getAdditionalModelProperties();
}
