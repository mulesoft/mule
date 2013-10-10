/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.context.notification.ServerNotificationManager;

import javax.resource.spi.work.WorkListener;

/**
 * Builder that is used to build instances of {@link MuleContext}. Implementing
 * classes are stateful and should provide public chainable setters for configuring
 * the builder instance and no public getters.
 */
public interface MuleContextBuilder
{
    /**
     * Builds a new {@link MuleContext} instance using configured builder instance.
     * Does not initialise or start MuleContext, only constructs the instance.
     */
    MuleContext buildMuleContext();

    void setWorkManager(WorkManager workManager);

    void setWorkListener(WorkListener workListener);
    
    void setNotificationManager(ServerNotificationManager notificationManager);

    void setLifecycleManager(LifecycleManager lifecycleManager);
    
    void setMuleConfiguration(MuleConfiguration muleConfiguration);
}
