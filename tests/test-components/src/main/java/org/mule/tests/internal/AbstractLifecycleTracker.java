/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.internal;

import static java.lang.String.format;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.tests.api.LifecycleTrackerRegistry;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public abstract class AbstractLifecycleTracker implements Lifecycle, MuleContextAware {

    @Inject
    private LifecycleTrackerRegistry registry;

    private final boolean shouldCheck;

    private MuleContext muleContext = null;
    private final List<String> alreadyCalledPhases = new ArrayList<>();

    public AbstractLifecycleTracker(boolean shouldCheck) {
        this.shouldCheck = shouldCheck;
    }

    protected void onInit(MuleContext muleContext) throws InitialisationException {
    }

    protected void onStart() throws MuleException {
    }

    protected void onStop() throws MuleException {
    }

    protected void onDispose() {
    }

    protected void addTrackingDataToRegistry(String trackerName) {
        registry.add(trackerName, alreadyCalledPhases);
    }

    private void trackPhase(String phase) {
        if (shouldCheck && alreadyCalledPhases.contains(phase)) {
            throw new IllegalStateException(format("Invalid phase transition: %s -> %s", alreadyCalledPhases.toString(), phase));
        }
        alreadyCalledPhases.add(phase);
    }

    @Override
    public void initialise() throws InitialisationException {
        trackPhase(Initialisable.PHASE_NAME);
        onInit(muleContext);
    }

    @Override
    public void start() throws MuleException {
        trackPhase(Startable.PHASE_NAME);
        onStart();
    }

    @Override
    public void stop() throws MuleException {
        trackPhase(Stoppable.PHASE_NAME);
        onStop();
    }

    @Override
    public void dispose() {
        trackPhase(Disposable.PHASE_NAME);
        onDispose();
    }

    @Override
    public void setMuleContext(MuleContext context) {
        if (muleContext == null) {
            alreadyCalledPhases.add("setMuleContext");
        }
        muleContext = context;
    }
}
