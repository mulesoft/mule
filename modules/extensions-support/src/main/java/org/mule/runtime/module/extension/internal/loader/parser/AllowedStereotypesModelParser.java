/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.List;

/**
 * General contract for a model parser capable of reading a component's allowed stereotypes
 *
 * @since 4.5.0
 */
public interface AllowedStereotypesModelParser {

  /**
   * Returns a {@link List} of allowed stereotypes. Each item is created through the provided {@code factory}.
   *
   * @param factory a {@link StereotypeModelFactory}
   * @return a list. Might be empty but will never be {@code null}
   */
  List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory);
}
