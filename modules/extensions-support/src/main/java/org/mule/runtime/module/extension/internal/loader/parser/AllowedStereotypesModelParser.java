/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
