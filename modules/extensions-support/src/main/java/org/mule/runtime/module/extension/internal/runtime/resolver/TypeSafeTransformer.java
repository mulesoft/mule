/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.exception.MessagingException;

/**
 * Utility class for {@link ValueResolver} to handle transformation of values
 *
 * @since 4.0
 */
public class TypeSafeTransformer {

  private MuleContext muleContext;

  TypeSafeTransformer(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * Given a {@code value) it will try to transform it to the expected type defined in the {@code expectedDataType}
   *
   * @param value the value to transform
   * @param valueDataType the value's {@link DataType}
   * @param expectedDataType the expected type's {@link DataType}
   * @param event the event to perform the transformation
   * @return the transformed value
   * @throws MessagingException If could not be able to find a proper transformer do obtain the desired type
   * @throws MessageTransformerException If a problem occurs transforming the value
   * @throws TransformerException If a problem occurs transforming the value
   */
  public Object transform(Object value, DataType valueDataType, DataType expectedDataType, Event event)
      throws MessagingException, MessageTransformerException, TransformerException {
    Transformer transformer;
    if (value != null) {
      try {
        transformer = muleContext.getRegistry().lookupTransformer(valueDataType, expectedDataType);
      } catch (TransformerException e) {
        throw new MessagingException(createStaticMessage(String.format(
                                                                       "Expression '%s' was expected to return a value of type '%s' but a '%s' was found instead "
                                                                           + "and no suitable transformer could be located",
                                                                       value.toString(), expectedDataType.getType().getName(),
                                                                       value.getClass().getName())),
                                     event, e);
      }

      Object result;
      if (transformer instanceof MessageTransformer) {
        result = ((MessageTransformer) transformer).transform(value, event);
      } else {
        result = transformer.transform(value);
      }
      return result;
    }
    return null;
  }
}

