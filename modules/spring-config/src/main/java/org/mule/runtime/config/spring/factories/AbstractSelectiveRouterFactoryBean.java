/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.internal.routing.AbstractSelectiveRouter;
import org.mule.runtime.core.internal.routing.MessageProcessorExpressionPair;

import java.util.Collection;

import org.springframework.beans.factory.FactoryBean;

public abstract class AbstractSelectiveRouterFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<AbstractSelectiveRouter>, MuleContextAware {

  private MessageProcessorChain defaultProcessor;
  private Collection<MessageProcessorExpressionPair> conditionalMessageProcessors;
  private MuleContext muleContext;

  public AbstractSelectiveRouterFactoryBean() {
    super();
  }

  public void setDefaultRoute(MessageProcessorExpressionPair conditionalProcessor) {
    defaultProcessor = conditionalProcessor.getMessageProcessor();
  }

  public void setRoutes(Collection<MessageProcessorExpressionPair> conditionalMessageProcessors) {
    this.conditionalMessageProcessors = conditionalMessageProcessors;
  }

  @Override
  public AbstractSelectiveRouter getObject() throws Exception {
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

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
