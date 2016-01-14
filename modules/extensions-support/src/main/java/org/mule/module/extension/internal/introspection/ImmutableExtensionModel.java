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
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.exception.NoSuchConfigurationException;
import org.mule.extension.api.exception.NoSuchMessageSourceException;
import org.mule.extension.api.exception.NoSuchOperationException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.EnrichableModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.SourceModel;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
    private final Map<String, SourceModel> messageSources;
    private final String vendor;
    private final Optional<ExceptionEnricherFactory> exceptionEnricherFactory;


    /**
     * Creates a new instance with the given state
     *
     * @param name                the extension's name. Cannot be blank
     * @param description         the extension's description
     * @param version             the extension's version
     * @param vendor              the extension's vendor name
     * @param configurationModels a {@link List} with the extension's {@link ConfigurationModel configurationModels}
     * @param operationModels     a {@link List} with the extension's {@link OperationModel operationModels}
     * @param connectionProviders a {@link List} with the extension's {@link ConnectionProviderModel connection provider models}
     * @param sourceModels        a {@link List} with the extension's {@link SourceModel message source models}
     * @param modelProperties     A {@link Map} of custom properties which extend this model
     * @param exceptionEnricherFactory    an Optional @{@link ExceptionEnricherFactory} that creates a concrete {@link org.mule.extension.api.introspection.ExceptionEnricher} instance
     * @throws IllegalArgumentException if {@code configurations} or {@link ParameterModel} are {@code null} or contain instances with non unique names, or if {@code name} is blank
     */
    protected ImmutableExtensionModel(String name,
                                      String description,
                                      String version,
                                      String vendor,
                                      List<ConfigurationModel> configurationModels,
                                      List<OperationModel> operationModels,
                                      List<ConnectionProviderModel> connectionProviders,
                                      List<SourceModel> sourceModels,
                                      Map<String, Object> modelProperties,
                                      Optional<ExceptionEnricherFactory>  exceptionEnricherFactory)
    {
        super(name, description, modelProperties);
        this.configurations = toMap(configurationModels);
        this.operations = toMap(operationModels);
        this.connectionProviders = immutableList(connectionProviders);
        this.exceptionEnricherFactory = exceptionEnricherFactory;
        this.messageSources = toMap(sourceModels);

        checkArgument(!StringUtils.isBlank(version), "Version cannot be blank");

        if (vendor == null)
        {
            throw new IllegalModelDefinitionException("Vendor cannot be null");
        }

        this.version = version;
        this.vendor = vendor;
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
        return findModel(configurations, name, () -> new NoSuchConfigurationException(this, name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<OperationModel> getOperationModels()
    {
        return ImmutableList.copyOf(operations.values());
    }

    @Override
    public List<SourceModel> getSourceModels()
    {
        return ImmutableList.copyOf(messageSources.values());
    }

    @Override
    public SourceModel getSourceModel(String name) throws NoSuchMessageSourceException
    {
        return findModel(messageSources, name, () -> new NoSuchMessageSourceException(this, name));
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
        return findModel(operations, name, () -> new NoSuchOperationException(this, name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConnectionProviderModel> getConnectionProviders()
    {
        return connectionProviders;
    }

    private <T extends EnrichableModel> T findModel(Map<String, T> map, String name, Supplier<RuntimeException> exception)
    {
        T model = map.get(name);
        if (model == null)
        {
            throw exception.get();
        }

        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVendor()
    {
        return vendor;
    }

    @Override
    public Optional<ExceptionEnricherFactory> getExceptionEnricherFactory()
    {
        return exceptionEnricherFactory;
    }
}
