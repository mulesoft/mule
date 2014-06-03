/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import java.util.List;

/**
 * A definition of an operation in a {@link org.mule.extensions.introspection.api.MuleExtension}
 *
 * @since 1.0
 */
public interface MuleExtensionOperation extends Described
{

    /**
     * Returns the parameters that this operation takes.
     *
     * @return an immutable {@link java.util.List} with instances of
     * {@link org.mule.extensions.introspection.api.MuleExtensionParameter}. It might be
     * empty if the operation takes no parameters, but it will never be {@code null}
     */
    List<MuleExtensionParameter> getParameters();

    /**
     * Returns the data types that are considered valid as input
     * for this operation. Notice that what this operation considers as input
     * is the payload of the current message. This does not refer to the parameters
     * returned by {@link #getParameters()}
     * <p/>
     * This list will not be {@code null} and will have at least one element.
     * If the operation can take any type or simply does not care about the payload because
     * it works solely on the parameters, then it must contain
     * at least the {@link java.lang.Class} corresponding to the {@link java.lang.Object} type
     *
     * @return an immutable {@link java.util.List} containing instances of {@link java.lang.Class}.
     */
    List<Class<?>> getInputTypes();

    /**
     * Returns the possible output types for this operation.
     * This list will not be {@code null} and will have at least one element
     *
     * @return a immutable {@link java.util.List} with instances of {@link java.lang.Class}
     */
    List<Class<?>> getOutputTypes();

    /**
     * Whether or not this operation is available for the given {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}.
     * <p/>
     * By default, all operations are available on all configurations and this method always returns {@code true}. However,
     * it is possible to limit operations to specific configurations only, in which case this method becomes relevant
     *
     * @param muleExtensionConfiguration a not {@code null} {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}
     * @return a boolean value
     */
    boolean isAvailableFor(MuleExtensionConfiguration muleExtensionConfiguration);
}
