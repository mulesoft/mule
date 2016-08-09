/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

/**
 * Inject an objects lifecycle state. This is useful for services that need to track or assert lifecycle state such as init,
 * dispose start, stop, dispose.
 *
 * @since 3.0
 */
public interface LifecycleStateAware {

  void setLifecycleState(LifecycleState state);
}
