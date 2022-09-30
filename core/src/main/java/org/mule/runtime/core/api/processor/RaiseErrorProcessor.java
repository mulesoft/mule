/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Processor capable of raising errors on demand, given a type and optionally a message.
 *
 * @since 4.0
 */
public final class RaiseErrorProcessor extends AbstractRaiseErrorProcessor {

  @Override
  protected ComponentIdentifier calculateErrorIdentifier(String typeId) {
    return buildFromStringRepresentation(typeId);
  }

  @Override
  protected MuleRuntimeException getException(ErrorType type, String message, CoreEvent event) {
    return new TypedException(new DefaultMuleException(message), type);
  }
}
