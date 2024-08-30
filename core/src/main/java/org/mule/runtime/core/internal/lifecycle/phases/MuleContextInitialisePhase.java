/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle.phases;

import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

/**
 * The MuleContextInitialisePhase defines the lifecycle behaviour when the Mule context is initialised. The MuleContext is
 * associated with one or more registries that inherit the lifecycle of the MuleContext.
 * <p/>
 * This phase is responsible for initialising objects. Any object that implements {@link Initialisable} will have its
 * {@link Initialisable#initialise()} method called. Objects are initialised in the order based on type:
 * {@link org.mule.runtime.core.api.construct.FlowConstruct}, followed by any other object that implements {@link Initialisable}.
 *
 * @see org.mule.runtime.core.api.MuleContext
 * @see org.mule.runtime.core.api.lifecycle.LifecycleManager
 * @see Initialisable
 * @since 3.0
 */
public class MuleContextInitialisePhase extends DefaultLifecyclePhase {

  public MuleContextInitialisePhase() {
    super(Initialisable.PHASE_NAME, LifecycleUtils::initialiseIfNeeded);
    registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
    setOrderedLifecycleTypes(new Class<?>[] {
        StreamingManager.class,
        ObjectStore.class,
        ExpressionLanguage.class,
        ConfigurationProvider.class,
        Config.class,
        SecurityManager.class,
        FlowConstruct.class,
        Initialisable.class
    });
    setIgnoredObjectTypes(new Class[] {
        MuleContext.class,
        ObjectProvider.class
    });
  }

  @Override
  public void applyLifecycle(Object o) throws LifecycleException {
    // retain default Lifecycle behaviour
    super.applyLifecycle(o);
    if (o == null) {
      return;
    }
    if (ignoreType(o.getClass())) {
      return;
    }
  }
}
