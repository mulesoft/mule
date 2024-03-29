/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.registry;

import org.mule.runtime.core.internal.registry.Registry;

/**
 * The parent interface for all object processors. Object processors can be registered in the Mule registry and fired at the
 * correct time.
 * <p/>
 * Developers must not implement this interface directly. Instead use either
 * {@link org.mule.runtime.core.privileged.registry.InjectProcessor} or
 * {@link org.mule.runtime.core.privileged.registry.PreInitProcessor}.
 *
 * @deprecated as of 3.7.0 since these are only used by {@link Registry} which is also deprecated. Use post processors for
 *             currently supported registries instead
 */
@Deprecated
// TODO W-10781591 Remove this
public interface ObjectProcessor {

  Object process(Object object);
}
