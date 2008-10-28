package org.mule.test.integration.components;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerComponent extends AbstractLifecycleTracker
        implements Callable {

    public void springInitialize() {
        getTracker().add("springInitialize");
    }

    public void springDestroy() {
        getTracker().add("springDestroy");
    }

    public Object onCall(final MuleEventContext eventContext) throws Exception {
        // dirty trick to get the component instance that was used for the
        // request
        return this;
    }

}
