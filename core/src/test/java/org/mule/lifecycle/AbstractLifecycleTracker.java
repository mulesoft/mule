/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Dossot (david@dossot.net)
 */
public abstract class AbstractLifecycleTracker implements Lifecycle,
        MuleContextAware {

    private final List<String> tracker = new ArrayList<String>();

    public List<String> getTracker() {
        return tracker;
    }

    public void setProperty(final String value) {
        getTracker().add("setProperty");
    }

    public void setMuleContext(final MuleContext context) {
        getTracker().add("setMuleContext");
    }

    public void initialise() throws InitialisationException {
        getTracker().add("initialise");
    }

    public void start() throws MuleException {
        getTracker().add("start");
    }

    public void stop() throws MuleException {
        getTracker().add("stop");
    }

    public void dispose() {
        getTracker().add("dispose");
    }

}
