/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.alphaSortDescribedList;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.OperationExecutorFactory;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.runtime.executor.OperationExecutorFactoryWrapper;
import org.mule.util.ValueHolder;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ExtensionFactory} which uses a
 * {@link ServiceRegistry} to locate instances of {@link ModelEnricher}.
 * <p>
 * <p>
 * The discovery of {@link ModelEnricher} instances  will happen when the
 * {@link #DefaultExtensionFactory(ServiceRegistry, ClassLoader)} constructor is invoked
 * and the list of discovered instances will be used during the whole duration of this instance
 *
 * @since 3.7.0
 */
public final class DefaultExtensionFactory implements ExtensionFactory
{

    private final List<ModelEnricher> modelEnrichers;

    /**
     * Creates a new instance and uses the given {@code serviceRegistry} to
     * locate instances of {@link ModelEnricher}
     *
     * @param serviceRegistry a {@link ServiceRegistry}
     * @param classLoader     the {@link ClassLoader} on which the {@code serviceRegistry} will search into
     */
    public DefaultExtensionFactory(ServiceRegistry serviceRegistry, ClassLoader classLoader)
    {
        modelEnrichers = ImmutableList.copyOf(serviceRegistry.lookupProviders(ModelEnricher.class, classLoader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtensionModel createFrom(Descriptor descriptor)
    {
        return createFrom(descriptor, new DefaultDescribingContext(descriptor.getRootDeclaration()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtensionModel createFrom(Descriptor descriptor, DescribingContext describingContext)
    {
        enrichModel(describingContext);
        return toExtension(descriptor.getRootDeclaration().getDeclaration());
    }

    private ExtensionModel toExtension(Declaration declaration)
    {
        validateMuleVersion(declaration);
        ValueHolder<ExtensionModel> extensionModelValueHolder = new ValueHolder<>();
        ExtensionModel extensionModel = new ImmutableExtensionModel(declaration.getName(),
                                                                    declaration.getDescription(),
                                                                    declaration.getVersion(),
                                                                    sortConfigurations(toConfigurations(declaration.getConfigurations(), extensionModelValueHolder)),
                                                                    alphaSortDescribedList(toOperations(declaration.getOperations())),
                                                                    declaration.getModelProperties());

        extensionModelValueHolder.set(extensionModel);
        return extensionModel;
    }

    private List<ConfigurationModel> sortConfigurations(List<ConfigurationModel> configurationModels)
    {
        List<ConfigurationModel> sorted = new ArrayList<>(configurationModels.size());

        // first one is kept as default while the rest are alpha sorted
        sorted.add(configurationModels.get(0));

        if (configurationModels.size() > 1)
        {
            sorted.addAll(alphaSortDescribedList(configurationModels.subList(1, configurationModels.size())));
        }

        return sorted;
    }


    private List<ConfigurationModel> toConfigurations(List<ConfigurationDeclaration> declarations, ValueHolder<ExtensionModel> extensionModelValueHolder)
    {
        checkArgument(!declarations.isEmpty(), "A extension must have at least one configuration");
        return declarations.stream()
                .map(declaration -> toConfiguration(declaration, extensionModelValueHolder))
                .collect(Collectors.toList());
    }

    private ConfigurationModel toConfiguration(ConfigurationDeclaration declaration, ValueHolder<ExtensionModel> extensionModel)
    {
        return new ImmutableConfigurationModel(declaration.getName(),
                                               declaration.getDescription(),
                                               extensionModel::get,
                                               declaration.getConfigurationInstantiator(),
                                               toConfigParameters(declaration.getParameters()),
                                               declaration.getModelProperties(),
                                               declaration.getInterceptorFactories());
    }

    private List<OperationModel> toOperations(List<OperationDeclaration> declarations)
    {
        if (declarations.isEmpty())
        {
            return ImmutableList.of();
        }

        return declarations.stream().map(this::toOperation).collect(Collectors.toList());
    }

    private OperationModel toOperation(OperationDeclaration declaration)
    {
        List<ParameterModel> parameterModels = toOperationParameters(declaration.getParameters());

        List<Interceptor> interceptors = createInterceptors(declaration.getInterceptorFactories());
        OperationExecutorFactory executorFactory = new OperationExecutorFactoryWrapper(declaration.getExecutorFactory(), interceptors);

        return new ImmutableOperationModel(declaration.getName(),
                                           declaration.getDescription(),
                                           executorFactory,
                                           parameterModels,
                                           declaration.getModelProperties(),
                                           declaration.getInterceptorFactories());
    }

    private List<ParameterModel> toConfigParameters(List<ParameterDeclaration> declarations)
    {

        List<ParameterModel> parameterModels = toParameters(declarations);
        alphaSortDescribedList(parameterModels);

        return parameterModels;
    }

    private List<ParameterModel> toOperationParameters(List<ParameterDeclaration> declarations)
    {
        return toParameters(declarations);
    }

    private List<ParameterModel> toParameters(List<ParameterDeclaration> declarations)
    {
        if (declarations.isEmpty())
        {
            return ImmutableList.of();
        }

        return declarations.stream().map(this::toParameter).collect(Collectors.toList());
    }

    private ParameterModel toParameter(ParameterDeclaration parameter)
    {
        return new ImmutableParameterModel(parameter.getName(),
                                           parameter.getDescription(),
                                           parameter.getType(),
                                           parameter.isRequired(),
                                           parameter.isDynamic(),
                                           parameter.getDefaultValue(),
                                           parameter.getModelProperties());
    }

    private void validateMuleVersion(Declaration declaration)
    {
        try
        {
            new MuleVersion(declaration.getVersion());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException(String.format("Invalid version %s for extension '%s'", declaration.getVersion(), declaration.getName()));
        }
    }

    private void enrichModel(DescribingContext describingContext)
    {
        modelEnrichers.forEach(enricher -> enricher.enrich(describingContext));
    }
}
