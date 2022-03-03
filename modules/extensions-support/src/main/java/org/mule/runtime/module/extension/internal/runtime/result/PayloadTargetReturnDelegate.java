/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.result;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * An implementation of {@link AbstractReturnDelegate} which sets the operation output as a variable which key is taken from the
 * {@link #target} field.
 * <p>
 * The original message payload is not modified.
 *
 * @since 4.3.0
 */
public final class PayloadTargetReturnDelegate extends AbstractReturnDelegate {

  private final String target;

  /**
   * {@inheritDoc}
   *
   * @param target the name of the variable in which the output message will be set
   */
  public PayloadTargetReturnDelegate(String target,
                                     ComponentModel componentModel,
                                     CursorProviderFactory cursorProviderFactory,
                                     MuleContext muleContext) {
    super(componentModel, cursorProviderFactory, muleContext);
    this.target = target;
  }

  @Override
  public CoreEvent asReturnValue(Object value, ExecutionContextAdapter operationContext) {
    return CoreEvent.builder(operationContext.getEvent())
        .securityContext(operationContext.getSecurityContext())
        .addVariable(target, toMessage(value, operationContext).getPayload())
        .build();
  }
}
