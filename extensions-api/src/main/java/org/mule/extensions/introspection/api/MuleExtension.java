/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import org.mule.extensions.api.exception.NoSuchConfigurationException;
import org.mule.extensions.api.exception.NoSuchOperationException;

import java.util.List;

/**
 * A Mule Extension, which describes functionality that extends the ESB.
 * Extensions export a set of {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}s.
 * Each configuration defines an independent access point for operations.
 *
 * @since 1.0
 */
public interface MuleExtension extends Described, Capable
{

    /**
     * The minimum Mule version to which this API remains compatible.
     * Any extension built using this API is guaranteed to be compatible with any Mule
     * Runtime equals or posterior to this one
     */
    public static final String MIN_MULE_VERSION = "3.6.0";

    /**
     * Returns this extension's version
     *
     * @return the version as a {@link java.lang.String}
     */
    String getVersion();

    /**
     * Returns the minimum Mule Version for which this extension is compatible.
     * Although this version needs to be at least {@link #MIN_MULE_VERSION},
     * it is possible to define a extension which is only compatible with a version
     * posterior to that
     */
    String getMinMuleVersion();

    /**
     * Returns this extension's type
     *
     * @return a not {@code null} {@link org.mule.extensions.introspection.api.MuleExtensionType}
     */
    MuleExtensionType getExtensionType();

    /**
     * Returns the {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}s
     * available for this extension. Each configuration is guaranteed to have a unique name
     *
     * @return an immutable {@link java.util.List} with the
     * available {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}s. This
     * list will always contain at least one element (the default configuration)
     */
    List<MuleExtensionConfiguration> getConfigurations();

    /**
     * Returns the {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}
     * that matches the given name.
     *
     * @param name case sensitive configuration name
     * @return a {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}
     * @throws NoSuchConfigurationException if no configuration available for that name
     */
    MuleExtensionConfiguration getConfiguration(String name) throws NoSuchConfigurationException;

    /**
     * Returns the {@link org.mule.extensions.introspection.api.MuleExtensionOperation}
     * available for this extension. This includes all operations,
     * regardless of which {@link org.mule.extensions.introspection.api.MuleExtensionConfiguration}s
     * each of those is available for. Each operation is guaranteed to have a unique name
     *
     * @return an immutable {@link java.util.List} of {@link org.mule.extensions.introspection.api.MuleExtensionOperation}
     */
    List<MuleExtensionOperation> getOperations();

    /**
     * Returns the {@link org.mule.extensions.introspection.api.MuleExtensionOperation} that matches
     * the given name.
     *
     * @param name case sensitive operation name
     * @return a {@link org.mule.extensions.introspection.api.MuleExtensionOperation}
     * @throws NoSuchOperationException if no operation matches the given name
     */
    MuleExtensionOperation getOperation(String name) throws NoSuchOperationException;

}
