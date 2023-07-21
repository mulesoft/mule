/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
