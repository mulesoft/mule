/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.function;

import org.mule.runtime.api.el.ExpressionFunction;

/**
 * A facade interface which hides the details of how a
 * function is actually executed. It aims to decouple
 * the abstract introspection model that the extension's
 * API proposes from the implementation details of the
 * underlying environment.
 *
 * @since 4.0
 */
public interface FunctionExecutor extends ExpressionFunction {

}
