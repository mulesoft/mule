/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.function;

import org.mule.runtime.api.el.ExpressionFunction;

/**
 * A facade interface which hides the details of how a function is actually executed. It aims to decouple the abstract
 * introspection model that the extension's API proposes from the implementation details of the underlying environment.
 *
 * @since 4.0
 */
public interface FunctionExecutor extends ExpressionFunction {

}
