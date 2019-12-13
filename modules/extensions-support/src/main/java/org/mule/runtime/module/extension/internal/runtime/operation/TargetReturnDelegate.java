/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.api.message.Message.of;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * An implementation of {@link AbstractReturnDelegate} which evaluates the {@link #targetValue} expressions and sets the output
 * as a variable which key is taken from the {@link #target} field.
 * <p>
 * The original message payload is not modified.
 *
 * @since 4.0
 */
final class TargetReturnDelegate extends AbstractReturnDelegate {

  private final ExpressionManager expressionManager;

  private final String target;
  private final CompiledExpression targetValue;

  /**
   * {@inheritDoc}
   *
   * @param target the name of the variable in which the output message will be set
   */
  TargetReturnDelegate(String target,
                       String targetValue,
                       ComponentModel componentModel,
                       ExpressionManager expressionManager,
                       CursorProviderFactory cursorProviderFactory,
                       MuleContext muleContext) {
    super(componentModel, cursorProviderFactory, muleContext);
    this.expressionManager = expressionManager;
    this.target = target;
    this.targetValue = expressionManager.compile(targetValue, getTargetBindingContext(of("")));
  }

  @Override
  public CoreEvent asReturnValue(Object value, ExecutionContextAdapter operationContext) {
    try (ExpressionLanguageSession session =
        expressionManager.openSession(getTargetBindingContext(toMessage(value, operationContext)))) {
      return CoreEvent.builder(operationContext.getEvent())
          .securityContext(operationContext.getSecurityContext())
          .addVariable(target, session.evaluate(targetValue))
          .build();
    }
  }
}
