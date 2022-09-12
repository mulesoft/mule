/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
