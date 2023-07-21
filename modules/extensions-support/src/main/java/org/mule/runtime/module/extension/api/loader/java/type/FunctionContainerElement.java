/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * A contract for a element from which operation container can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface FunctionContainerElement extends Type, WithFunctions {

}
