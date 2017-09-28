/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.AbstractSelectiveRouter;
import org.mule.runtime.core.internal.routing.MessageProcessorExpressionPair;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.Collection;

import javax.inject.Inject;

public abstract class AbstractSelectiveRouterObjectFactory extends AbstractComponentFactory<AbstractSelectiveRouter> {

  @Inject
  private MuleContext muleContext;

  private Processor defaultProcessor;
  private Collection<MessageProcessorExpressionPair> conditionalMessageProcessors;

  public AbstractSelectiveRouterObjectFactory() {
    super();
  }

  public void setDefaultRoute(MessageProcessorExpressionPair conditionalProcessor) {
    defaultProcessor = conditionalProcessor.getMessageProcessor();
  }

  public void setRoutes(Collection<MessageProcessorExpressionPair> conditionalMessageProcessors) {
    this.conditionalMessageProcessors = conditionalMessageProcessors;
  }

  @Override
  public AbstractSelectiveRouter doGetObject() throws Exception {
    final AbstractSelectiveRouter router = newAbstractSelectiveRouter();
    router.setAnnotations(getAnnotations());
    router.setDefaultRoute(defaultProcessor);
    router.setMuleContext(muleContext);

    for (final MessageProcessorExpressionPair mpfp : conditionalMessageProcessors) {
      router.addRoute(mpfp.getExpression(), mpfp.getMessageProcessor());
    }

    return router;
  }

  protected abstract AbstractSelectiveRouter newAbstractSelectiveRouter();

}
