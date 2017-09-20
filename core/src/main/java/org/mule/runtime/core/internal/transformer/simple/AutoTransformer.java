/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;

import java.nio.charset.Charset;

/**
 * A transformer that uses the transform discovery mechanism to convert the message payload. This transformer works much better
 * when transforming custom object types rather that java types since there is less chance for ambiguity. If an exact match cannot
 * be made an exception will be thrown.
 */
public class AutoTransformer extends AbstractMessageTransformer {

  /**
   * Template method where deriving classes can do any initialisation after the properties have been set on this transformer
   *
   * @throws InitialisationException
   *
   */
  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    if (Object.class.equals(getReturnDataType().getType())) {
      throw new InitialisationException(CoreMessages.transformerInvalidReturnType(Object.class, getName()), this);
    }
  }

  @Override
  public Object transformMessage(CoreEvent event, Charset outputEncoding) throws MessageTransformerException {
    return muleContext.getTransformationService()
        .transform(event.getMessage(), DataType.fromType(getReturnDataType().getType()))
        .getPayload().getValue();
  }
}
