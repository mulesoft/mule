/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;

import org.slf4j.Logger;

public class RouteBuilderValueResolver extends ObjectBuilderValueResolver implements Lifecycle {

  private static final Logger LOGGER = getLogger(RouteBuilderValueResolver.class);

  private MessageProcessorChain nestedChain;

  public RouteBuilderValueResolver(ObjectBuilder builder, MuleContext muleContext, MessageProcessorChain chain) {
    super(builder, muleContext);
    this.nestedChain = chain;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    if (nestedChain != null) {
      initialiseIfNeeded(nestedChain, muleContext);
    }
  }

  @Override
  public void dispose() {
    if (nestedChain != null) {
      disposeIfNeeded(nestedChain, LOGGER);
    }
  }

  @Override
  public void start() throws MuleException {
    if (nestedChain != null) {
      startIfNeeded(nestedChain);
    }
  }

  @Override
  public void stop() throws MuleException {
    if (nestedChain != null) {
      stopIfNeeded(nestedChain);
    }
  }
}
