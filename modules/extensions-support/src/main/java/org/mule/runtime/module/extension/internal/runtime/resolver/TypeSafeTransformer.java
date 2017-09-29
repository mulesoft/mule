/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;

/**
 * Utility class for {@link ValueResolver} to handle transformation of values
 *
 * @since 4.0
 */
public class TypeSafeTransformer {

  private TransformationService transformationService;

  public TypeSafeTransformer(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  /**
   * Given a {@code value) it will try to transform it to the expected type defined in the {@code expectedDataType}
   *
   * @param value the value to transform
   * @param valueDataType the value's {@link DataType}
   * @param expectedDataType the expected type's {@link DataType}
   * @param event the event to perform the transformation
   * @return the transformed value
   * @throws MessageTransformerException If a problem occurs transforming the value
   * @throws TransformerException If a problem occurs transforming the value
   */
  public <T> T transform(Object value, DataType valueDataType, DataType expectedDataType)
      throws MessageTransformerException, TransformerException {

    // TODO review that event is not need but there was logic to use MessageTransform
    return (T) transformationService.transform(value, valueDataType, expectedDataType);
  }
}

