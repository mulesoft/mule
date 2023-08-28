/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.util.func.CheckedBiFunction;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * Transforms a component's output considering the state of an {@link ExecutionContextAdapter}
 *
 * @since 4.5.0
 */
@FunctionalInterface
public interface ResultTransformer extends CheckedBiFunction<ExecutionContextAdapter, Object, Object> {

}
