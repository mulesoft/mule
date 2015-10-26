/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.toMap;
import static org.mule.util.CollectionUtils.immutableList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extension.api.exception.NoSuchConfigurationException;
import org.mule.extension.api.exception.NoSuchOperationException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Immutable implementation of {@link ExtensionModel}
 *
 * @since 3.7.0
 */
final class ImmutableExtensionModel extends AbstractImmutableModel implements ExtensionModel
{

    private final String version;
    private final Map<String, ConfigurationModel> configurations;
    private final Map<String, OperationModel> operations;
    private final List<ConnectionProviderModel> connectionProviders;

    /**
     * Creates a new instance with the given state
     *
     * @param name                the extension's name. Cannot be blank
     * @param description         the extension's description
     * @param version             the extension's version
     * @param configurationModels a {@link List} with the extension's {@link ConfigurationModel configurationModels}
     * @param operationModels     a {@link List} with the extension's {@link OperationModel operationModels}
     * @param connectionProviders a {@link List} with the extension's {@link ConnectionProviderModel connection provider models}
     * @param modelProperties     A {@link Map} of custom properties which extend this model
     * @throws IllegalArgumentException if {@code configurations} or {@link ParameterModel} are {@code null} or contain instances with non unique names, or if {@code name} is blank
     */
    protected ImmutableExtensionModel(String name,
                                      String description,
                                      String version,
                                      List<ConfigurationModel> configurationModels,
                                      List<OperationModel> operationModels,
                                      List<ConnectionProviderModel> connectionProviders,
                                      Map<String, Object> modelProperties)
    {
        super(name, description, modelProperties);

        checkArgument(!name.contains(" "), "Extension name cannot contain spaces");

        this.configurations = toMap(configurationModels);
        this.operations = toMap(operationModels);
        this.connectionProviders = immutableList(connectionProviders);

        checkArgument(!StringUtils.isBlank(version), "version cannot be blank");
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConfigurationModel> getConfigurationModels()
    {
        return ImmutableList.copyOf(configurations.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationModel getConfigurationModel(String name) throws NoSuchConfigurationException
    {
        ConfigurationModel configurationModel = configurations.get(name);
        if (configurationModel == null)
        {
            throw new NoSuchConfigurationException(this, name);
        }

        return configurationModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OperationModel> getOperationModels()
    {
        return ImmutableList.copyOf(operations.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion()
    {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationModel getOperationModel(String name) throws NoSuchOperationException
    {
        OperationModel operationModel = operations.get(name);
        if (operationModel == null)
        {
            throw new NoSuchOperationException(this, name);
        }

        return operationModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConnectionProviderModel> getConnectionProviders()
    {
        return connectionProviders;
    }
}
