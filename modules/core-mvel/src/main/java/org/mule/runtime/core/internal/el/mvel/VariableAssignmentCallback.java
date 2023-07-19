/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * @since 3.3
 */

package org.mule.runtime.core.internal.el.mvel;

/**
 * @since 3.3
 */
public interface VariableAssignmentCallback<T> {

  void assignValue(String name, T value, T newValue);
}
