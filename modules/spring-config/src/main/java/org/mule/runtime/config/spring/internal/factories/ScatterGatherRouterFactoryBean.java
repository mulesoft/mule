/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.internal.factories;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.ScatterGatherRouter;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ScatterGatherRouterFactoryBean extends AbstractAnnotatedObject
    implements FactoryBean<ScatterGatherRouter>, MuleContextAware {

  private long timeout = 0;
  private List<Processor> messageProcessors;
  private MuleContext muleContext;
  private FlowConstruct flowConstruct;

  @Override
  public ScatterGatherRouter getObject() throws Exception {
    ScatterGatherRouter sg = new ScatterGatherRouter();
    sg.setTimeout(timeout);
    sg.setMuleContext(muleContext);

    for (Processor mp : this.messageProcessors) {
      sg.addRoute(mp);
    }

    sg.setAnnotations(getAnnotations());
    return sg;
  }

  @Override
  public Class<?> getObjectType() {
    return ScatterGatherRouter.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

}
