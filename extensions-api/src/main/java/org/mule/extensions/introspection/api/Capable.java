/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import com.google.common.base.Optional;

/**
 * An object is capable if it provides optional support for additional {@link org.mule.extensions.introspection.api.Capability}.
 * <p>
 * A capability is identified by a class type. Objects are not required to implement any
 * capability at all, even if they support this interface.
 * </p>
 * <p>
 * Capabilities are used to provide a future-proof path to incorporate changes that may otherwise
 * break backwards compatibility.
 * </p>
 *
 */
public interface Capable
{

    /**
     * Returns the capability associated with the {@code capability} if present, and absent if not.
     *
     * @param capabilityType the capability to be obtained.
     * @since 1.0
     */
    <T extends Capability> Optional<T> getCapability(Class<T> capabilityType);
}
