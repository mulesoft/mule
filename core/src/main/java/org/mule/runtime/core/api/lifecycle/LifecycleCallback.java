/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.exception.MuleException;

/**
 * This callback is used to execute lifecycle behaviour for an object being managed by a {@link LifecycleManager} The callback is
 * used so that transitions can be managed consistently outside of an object
 *
 * @since 3.0
 */
public interface LifecycleCallback<O> {

  void onTransition(String phaseName, O object) throws MuleException;
}
