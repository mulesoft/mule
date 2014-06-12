/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import com.google.common.base.Optional;

/**
 * An object is capable if it may provide different facet of additional information.
 * <p>
 * Capabilities provide a mechanism for specific extensions to the api that do not break clients but may provide additional
 * information on new (non-mandatory) aspects.
 *
 * <p>
 *     When building a new Capability, you should consider that it may not be recognized or used at all by existing
 *     consumers. This limits the use of this feature to non-critical stuff or to extended contracts targetting a specific
 *     consumer.
 * </p>
 * <p>
 *     Consumers of a capability must be prepared to deal with he fact that the object may not implement it
 * </p>
 * <p>
 *     Capabilities are used to provide a future-proof path to incorporate changes that may otherwise
 *     break backwards compatibility. New information can be provided in a new class exposed when specifically queried
 *     as a capability. Hence, clients that were not compiled with the new class in scope will not fail, and only
 *     clients aware of a specific capability will use it (that's the reason why there's no ennumeration of
 *     capabilities in the interface)
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
    <T> Optional<T> getCapability(Class<T> capabilityType);
}
