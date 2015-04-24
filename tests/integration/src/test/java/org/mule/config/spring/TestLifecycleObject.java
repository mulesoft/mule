/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.store.ObjectStoreManager;

import javax.inject.Inject;

public class TestLifecycleObject implements Lifecycle
{

    private int initialise = 0;
    private int start = 0;
    private int stop = 0;
    private int dispose = 0;

    @Inject
    private ObjectStoreManager objectStoreManager;

    @Inject
    private MuleContext muleContext;

    @Override
    public void initialise() throws InitialisationException
    {
        initialise++;
    }

    @Override
    public void start() throws MuleException
    {
        start++;
    }

    @Override
    public void stop() throws MuleException
    {
        stop++;
    }

    @Override
    public void dispose()
    {
        dispose++;
    }

    public int getInitialise()
    {
        return initialise;
    }

    public int getStart()
    {
        return start;
    }

    public int getStop()
    {
        return stop;
    }

    public int getDispose()
    {
        return dispose;
    }

    public ObjectStoreManager getObjectStoreManager()
    {
        return objectStoreManager;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }
}
