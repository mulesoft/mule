/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.core.internal.registry.Registry;

/**
 * Inject an objects lifecycle state. This is useful for services that need to track or assert lifecycle state such as init,
 * dispose start, stop, dispose.
 *
 * @deprecated as of 3.7.0 since these are only used by {@link Registry} which is also deprecated. Use post processors for
 *             currently supported registries instead
 * 
 * @since 3.0
 */
@Deprecated
// TODO W-10781591 Remove this
public interface LifecycleStateAware {

  void setLifecycleState(LifecycleState state);
}
