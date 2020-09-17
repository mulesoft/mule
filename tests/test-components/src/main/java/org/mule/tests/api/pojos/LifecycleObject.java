/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api.pojos;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.tests.api.LifecycleTrackerRegistry;
import org.mule.tests.internal.BaseLifecycleTracker;

import javax.inject.Inject;

@TypeDsl(allowTopLevelDefinition = true)
public class LifecycleObject extends BaseLifecycleTracker {

    @Inject
    private LifecycleTrackerRegistry registry;

    @RefName
    private String name;

    @Optional
    @Parameter
    private String failurePhase;

    public LifecycleObject() {
        super(false);
    }

    @Override
    protected void onSetMuleContext(MuleContext muleContext) {
        addTrackingDataToRegistry(name);
        failIfNeeded("setMuleContext");
    }

    @Override
    protected void onInit(MuleContext muleContext) throws InitialisationException {
        failIfNeeded(Initialisable.PHASE_NAME);
    }

    @Override
    protected void onStart() throws MuleException {
        failIfNeeded(Startable.PHASE_NAME);
    }

    @Override
    protected void onStop() throws MuleException {
        failIfNeeded(Stoppable.PHASE_NAME);
    }

    @Override
    protected void onDispose() {
        failIfNeeded(Disposable.PHASE_NAME);
    }

    private void failIfNeeded(String phase) {
        if (failurePhase != null && failurePhase.equalsIgnoreCase(phase)) {
            throw new RuntimeException("generated failure");
        }
    }
}
