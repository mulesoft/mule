/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

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
