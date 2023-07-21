/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public enum TestError implements ErrorTypeDefinition<TestError> {
  CHILD, PARENT
}
