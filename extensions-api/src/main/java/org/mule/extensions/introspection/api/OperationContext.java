/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

public interface OperationContext
{

    /**
     * Provides a value for a parameter.
     * @param name the parameter's name to write
     * @param value the value of the parameter
     * @return {@code this}
     * @throws IllegalArgumentException if the name doesn't match any parameter
     *
     * @since 1.0
     */
    OperationContext setParameter(String name, Object value);

    /**
     * Provides a value for a parameter.
     * This method will not fail if the value has an invalid type, or the actual value doesn't make sense at all. Validation will happen only
     * when building the instance.
     *
     * @param param the parameter to write
     * @param value the value of the parameter
     * @return {@code this}
     * @throws IllegalArgumentException if the parameter is not one of the allowed parameters
     *
     * @since 1.0
     */
    OperationContext setParameter(MuleExtensionParameter param, Object value);

    MuleExtensionOperation getOperation();

}
