/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.internal;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.RefName;

@Configuration(name = "lifecycleTrackerConfig")
@Operations(LifecycleTrackerOperations.class)
public class LifecycleTrackerConfiguration extends AbstractLifecycleTracker {

    @RefName
    private String configName;

    public LifecycleTrackerConfiguration() {
        super(false);
    }

    @Override
    protected void onInit(MuleContext muleContext) throws InitialisationException {
        addTrackingDataToRegistry(configName);
    }
}
