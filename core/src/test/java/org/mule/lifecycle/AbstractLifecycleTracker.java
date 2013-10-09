/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
