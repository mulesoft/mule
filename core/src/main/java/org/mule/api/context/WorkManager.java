/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;

import java.util.concurrent.Executor;

/**
 * <code>WorkManager</code> extends the standard JCA WorkManager with lifecycle
 * methods and util.concurrent bridging.
 */
public interface WorkManager extends javax.resource.spi.work.WorkManager, Startable, Disposable, Executor
{
    /** Is the WorkManager ready to accept work? */
    public boolean isStarted();
    
}
