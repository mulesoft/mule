/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerComponentObject extends AbstractLifecycleTracker
        implements ServiceAware, Callable {

    public void springInitialize() {
        getTracker().add("springInitialize");
    }

    public void springDestroy() {
        getTracker().add("springDestroy");
    }

     public void setService(final Service service)
    {
        getTracker().add("setService");
    }

    public Object onCall(final MuleEventContext eventContext) throws Exception {
        // dirty trick to get the component instance that was used for the
        // request
        return this;
    }

}
