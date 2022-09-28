/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.processor;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkExtensionModelParser.APP_LOCAL_EXTENSION_NAMESPACE;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractRaiseErrorProcessor;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

/**
 * Processor capable of raising errors within a Mule Operation's body on demand, given a type and optionally a message.
 *
 * @since 4.5
 */
public final class MuleSdkRaiseErrorProcessor extends AbstractRaiseErrorProcessor {

  private static final DataType ERROR_DATA_TYPE = fromType(Error.class);

  private AttributeEvaluator cause = new AttributeEvaluator("#[error]", ERROR_DATA_TYPE);

  @Override
  protected ComponentIdentifier calculateErrorIdentifier(String typeId) {
    return builder().namespace(APP_LOCAL_EXTENSION_NAMESPACE).name(typeId).build();
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    cause.initialize(expressionManager);
  }

  @Override
  protected TypedException getException(ErrorType type, String message, CoreEvent event) {
    Error calculatedCause = calculateCause(event);
    if (calculatedCause == null) {
      return new TypedException(new DefaultMuleException(message), type);
    } else {
      return new TypedException(new DefaultMuleException(message), type, message);
    }
  }

  private Error calculateCause(CoreEvent event) {
    return cause.<Error>resolveTypedValue(event).getValue();
  }

  public void setCause(String cause) {
    this.cause = new AttributeEvaluator(cause, ERROR_DATA_TYPE);
  }
}
