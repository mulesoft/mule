/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

public interface MuleExtensionParameter extends Described
{

    Class<?> getType();

    boolean isRequired();

    boolean isAcceptingExpressions();

    /**
     * Might be an expression is {@link #isAcceptingExpressions()}  is {@code true}
     * That's why this interface is not typed
     * @return
     */
    Object getDefaultValue();
}
