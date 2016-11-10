/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;

import java.util.concurrent.Executor;

/**
 * <code>WorkManager</code> extends the standard JCA WorkManager with lifecycle methods and util.concurrent bridging.
 */
public interface WorkManager extends javax.resource.spi.work.WorkManager, Startable, Disposable, Executor {

  /** Is the WorkManager ready to accept work? */
  public boolean isStarted();

}
