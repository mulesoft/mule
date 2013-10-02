/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * @since 3.3
 */

package org.mule.api.el;

/**
 * @since 3.3
 */
public interface VariableAssignmentCallback<T>
{
    public void assignValue(String name, T value, T newValue);
}
