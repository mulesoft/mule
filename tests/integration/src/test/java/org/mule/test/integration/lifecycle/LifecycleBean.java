/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;


import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;

public class LifecycleBean implements Lifecycle
{

    @Inject
    private MuleContext muleContext;

    private String failurePhase;
    private List<String> lifecycleInvocations = new ArrayList<>();

    public void setFailurePhase(String failurePhase)
    {
        this.failurePhase = failurePhase;
    }

    public List<String> getLifecycleInvocations()
    {
        return lifecycleInvocations;
    }

    private void failIfNeeded(String phase)
    {
        if (failurePhase != null && failurePhase.equalsIgnoreCase(phase))
        {
            throw new RuntimeException("generated failure");
        }
    }

    @Override
    public void stop() throws MuleException
    {
        lifecycleInvocations.add("stop");
        failIfNeeded("stop");
    }

    @Override
    public void dispose()
    {
        lifecycleInvocations.add("dispose");
        failIfNeeded("dispose");
    }

    @Override
    public void start() throws MuleException
    {
        lifecycleInvocations.add("start");
        failIfNeeded("start");
    }

    @Override
    public void initialise() throws InitialisationException
    {
        lifecycleInvocations.add("initialise");
        failIfNeeded("initialise");
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }
}