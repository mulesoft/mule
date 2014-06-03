/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

/**
 * Describes a user configured parameter in the context of a {@link org.mule.extensions.introspection.api.MuleExtension}.
 * <p/>
 * It can apply either to a {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration} or a
 * {@link org.mule.extensions.introspection.api.MuleExtensionOperation}
 *
 * @since 1.0
 */
public interface MuleExtensionParameter extends Described
{

    /**
     * Returns the type of the operation
     *
     * @return a not {@code null} {@link java.lang.Class}
     */
    Class<?> getType();

    /**
     * Whether or not this parameter is required. This method is exclusive with
     * {@link #getDefaultValue()} in the sense that a required parameter cannot have a default
     * value. At the same time, if the parameter has a default value, then it makes no sense
     * to consider it as required
     *
     * @return a boolean value saying if this parameter is required or not
     */
    boolean isRequired();

    /**
     * A parameter is considered to be dynamic if it's value can change in runtime
     * depending on contextual circumstances. E.g.: the parameter value is a MEL expression
     * or can be dynamically changed by a third party
     *
     * @return
     */
    boolean isDynamic();

    /**
     * The default value for this parameter. It might be an expression if
     * {@link #isDynamic()} returns {@code true}.
     * This method is exclusive with {@link #isRequired()}. Check that method's comments for
     * more information on the semantics of this two methods.
     *
     * @return the default value
     */
    Object getDefaultValue();
}
