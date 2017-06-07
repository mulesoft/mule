/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logging;

import java.beans.PropertyChangeListener;

/**
 * Observable that dispatches calls to registered listeners when the inner log configuration is changed.
 */
public interface LogConfigChangeSubject {

  /**
   * Register a listener to be called when log config is changed.
   * 
   * @param logConfigChangeListener the listener to register.
   */
  void registerLogConfigChangeListener(PropertyChangeListener logConfigChangeListener);

  /**
   * Unregister a listener to no longer be called when log config is changed.
   * 
   * @param logConfigChangeListener the listener to unregister.
   */
  void unregisterLogConfigChangeListener(PropertyChangeListener logConfigChangeListener);

}
