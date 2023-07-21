/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.registry;

import org.mule.runtime.core.internal.registry.Registry;

/**
 * Fired after the inject phase but before the initialise lifecycle phase
 *
 * @deprecated as of 3.7.0 since these are only used by {@link Registry} which is also deprecated. Use post processors for
 *             currently supported registries instead
 */
@Deprecated
// TODO W-10781591 Remove this
public interface PreInitProcessor extends ObjectProcessor {
}
