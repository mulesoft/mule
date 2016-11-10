/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.component;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Lifecycle;

/**
 * <code>LifecycleAdapter</code> is a wrapper around a pojo service that adds Lifecycle methods to the pojo.
 */
public interface LifecycleAdapter extends Lifecycle {

  boolean isStarted();

  boolean isDisposed();

  Object invoke(Event message, Event.Builder eventBuilder) throws MuleException;

}
