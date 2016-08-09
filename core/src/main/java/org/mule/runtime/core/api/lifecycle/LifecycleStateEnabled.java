/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

/**
 * Objects that have an associated lifecycle manager such as {@link org.mule.runtime.core.api.construct.FlowConstruct} should
 * implement this interface so that the registry lifecycle manager can introspect the lifecycle state of an object. This allows
 * objects to have a different lifecycle state to the registry. Typically objects that have their own lifecycle manager can be
 * controlled externally through JMX which means there is no guarantee that the state of the object is in the same lifecycle state
 * of the registry, hence the need to introspect the lifecycle state of an object.
 *
 * @since 3.0
 */
public interface LifecycleStateEnabled {

  LifecycleState getLifecycleState();
}
