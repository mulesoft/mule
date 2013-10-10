/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.api.service.Service;
import org.mule.api.transport.Connector;

/**
 * Objects that have an associated lifecycle manager such as {@link Service} or
 * {@link Connector} should implement this interface so that the registry lifecycle
 * manager can introspect the lifecycle state of an object. This allows objects to
 * have a different lifecycle state to the registry. Typically objects that have
 * their own lifecycle manager can be controlled externally through JMX which means
 * there is no guarantee that the state of the object is in the same lifecycle state
 * of the registry, hence the need to introspect the lifecycle state of an object.
 *
 * @since 3.0
 */
public interface LifecycleStateEnabled
{
    LifecycleState getLifecycleState();
}
