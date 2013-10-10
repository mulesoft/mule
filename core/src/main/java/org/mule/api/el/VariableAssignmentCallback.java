/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
