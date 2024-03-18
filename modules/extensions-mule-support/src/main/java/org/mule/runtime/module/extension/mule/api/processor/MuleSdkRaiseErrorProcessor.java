/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.processor;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.module.extension.mule.internal.error.ThrowableError.wrap;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkApplicationExtensionModelParser.APP_LOCAL_EXTENSION_NAMESPACE;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractRaiseErrorProcessor;
import org.mule.runtime.core.internal.util.attribute.AttributeEvaluator;

/**
 * Processor capable of raising errors within a Mule Operation's body on demand, given a type and optionally a message.
 *
 * @since 4.5
 */
@Experimental
public final class MuleSdkRaiseErrorProcessor extends AbstractRaiseErrorProcessor {

  private static final DataType ERROR_DATA_TYPE = fromType(Error.class);

  private AttributeEvaluator causeEvaluator = new AttributeEvaluator("#[error]", ERROR_DATA_TYPE);

  @Override
  protected ComponentIdentifier calculateErrorIdentifier(String typeId) {
    return builder().namespace(APP_LOCAL_EXTENSION_NAMESPACE).name(typeId).build();
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    causeEvaluator.initialize(expressionManager);
  }

  @Override
  protected TypedException getException(ErrorType type, String message, CoreEvent event) {
    return new TypedException(calculateCause(event, message), type);
  }

  private Throwable calculateCause(CoreEvent event, String message) {
    Error causeError = causeEvaluator.<Error>resolveTypedValue(event).getValue();
    if (causeError == null) {
      return new DefaultMuleException(message);
    } else {
      return wrap(causeError);
    }
  }

  public void setCause(String cause) {
    this.causeEvaluator = new AttributeEvaluator(cause, ERROR_DATA_TYPE);
  }
}
