/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.lifecycle.phases.NotInLifecyclePhase;
import org.mule.runtime.core.privileged.lifecycle.AbstractLifecycleManager;

/**
 * This is a specialized class that extends {@link RegistryLifecycleManager} and will invoke lifecycle on the registry instance
 * for the MuleContext. This class must only be used by the MuleContext.
 */
public class MuleContextLifecycleManager extends AbstractLifecycleManager<MuleContext> implements MuleContextAware {

  private MuleContext muleContext;

  private MuleContextLifecycleCallback callback = new MuleContextLifecycleCallback();

  public MuleContextLifecycleManager() {
    // We cannot pass in a MuleContext on creation since the context is not actually created when this object is needed
    super("MuleContext", null);
  }

  @Override
  protected void registerTransitions() {
    // init dispose
    addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
    addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Disposable.PHASE_NAME);
    addDirectTransition(Initialisable.PHASE_NAME, Startable.PHASE_NAME);
    addDirectTransition(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);

    // start stop
    addDirectTransition(Startable.PHASE_NAME, Stoppable.PHASE_NAME);
    addDirectTransition(Stoppable.PHASE_NAME, Startable.PHASE_NAME);
    addDirectTransition(Stoppable.PHASE_NAME, Disposable.PHASE_NAME);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    this.object = muleContext;
  }

  @Override
  public void fireLifecycle(String destinationPhase) throws LifecycleException {
    checkPhase(destinationPhase);
    invokePhase(destinationPhase, object, callback);
  }

  @Override
  protected void doOnConnectException(ConnectException ce) throws LifecycleException {
    throw new LifecycleException(ce, this);
  }

  class MuleContextLifecycleCallback implements LifecycleCallback<MuleContext> {

    @Override
    public void onTransition(String phaseName, MuleContext muleContext) throws MuleException {
      ((MuleContextWithRegistries) muleContext).getRegistry().fireLifecycle(phaseName);
    }
  }
}
