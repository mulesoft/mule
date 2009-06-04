/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.context;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

/**
 * <code>WorkManager</code> extends the standard JCA WorkManager with lifecycle
 * methods and util.concurrent bridging.
 */
public interface WorkManager extends javax.resource.spi.work.WorkManager, Startable, Disposable, Executor
{
    /** Is the WorkManager ready to accept work? */
    public boolean isStarted();
}
