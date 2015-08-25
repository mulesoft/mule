/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.alphaSortDescribedList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.ExtensionFactory;
import org.mule.extension.introspection.OperationModel;
import org.mule.extension.introspection.ParameterModel;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.introspection.declaration.fluent.Declaration;
import org.mule.extension.introspection.declaration.fluent.Descriptor;
import org.mule.extension.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.introspection.declaration.spi.DescriberPostProcessor;
import org.mule.module.extension.internal.DefaultDescribingContext;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ExtensionFactory} which uses a
 * {@link ServiceRegistry} to locate instances of {@link DescriberPostProcessor}.
 * The discovery of {@link DescriberPostProcessor}s will happen when the
 * {@link #DefaultExtensionFactory(ServiceRegistry)} constructor is invoked
 * and the list of discovered instances will be used during the whole duration of this instance
 *
 * @since 3.7.0
 */
public final class DefaultExtensionFactory implements ExtensionFactory
{

    private final List<DescriberPostProcessor> postProcessors;

    /**
     * Creates a new instance and uses the given {@code serviceRegistry} to
     * locate instances of {@link DescriberPostProcessor}
     *
     * @param serviceRegistry a [@link ServiceRegistry
     */
    public DefaultExtensionFactory(ServiceRegistry serviceRegistry)
    {
        postProcessors = searchPostProcessors(serviceRegistry);
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
        applyPostProcessors(describingContext);
        return toExtension(descriptor.getRootDeclaration().getDeclaration());
    }

    private ExtensionModel toExtension(Declaration declaration)
    {
        validateMuleVersion(declaration);
        return new ImmutableExtensionModel(declaration.getName(),
                                      declaration.getDescription(),
                                      declaration.getVersion(),
                                      sortConfigurations(toConfigurations(declaration.getConfigurations())),
                                      alphaSortDescribedList(toOperations(declaration.getOperations())),
                                      declaration.getCapabilities());
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


    private List<ConfigurationModel> toConfigurations(List<ConfigurationDeclaration> declarations)
    {
        checkArgument(!declarations.isEmpty(), "A extension must have at least one configuration");

        List<ConfigurationModel> configurationModels = new ArrayList<>(declarations.size());
        for (ConfigurationDeclaration declaration : declarations)
        {
            configurationModels.add(toConfiguration(declaration));
        }

        return configurationModels;
    }

    private ConfigurationModel toConfiguration(ConfigurationDeclaration declaration)
    {
        return new ImmutableConfigurationModel(declaration.getName(),
                                          declaration.getDescription(),
                                          declaration.getConfigurationInstantiator(),
                                          toConfigParameters(declaration.getParameters()),
                                          declaration.getCapabilities());
    }

    private List<OperationModel> toOperations(List<OperationDeclaration> declarations)
    {
        if (declarations.isEmpty())
        {
            return ImmutableList.of();
        }

        List<OperationModel> operationModels = new ArrayList<>(declarations.size());
        for (OperationDeclaration declaration : declarations)
        {
            operationModels.add(toOperation(declaration));
        }

        return operationModels;
    }

    private OperationModel toOperation(OperationDeclaration declaration)
    {
        List<ParameterModel> parameterModels = toOperationParameters(declaration.getParameters());
        return new ImmutableOperationModel(declaration.getName(),
                                      declaration.getDescription(),
                                      declaration.getExecutorFactory(),
                                           parameterModels,
                                      declaration.getCapabilities());
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

        List<ParameterModel> parameterModels = new ArrayList<>(declarations.size());
        for (ParameterDeclaration declaration : declarations)
        {
            parameterModels.add(toParameter(declaration));
        }

        return parameterModels;
    }

    private ParameterModel toParameter(ParameterDeclaration parameter)
    {
        return new ImmutableParameterModel(parameter.getName(),
                                      parameter.getDescription(),
                                      parameter.getType(),
                                      parameter.isRequired(),
                                      parameter.isDynamic(),
                                      parameter.getDefaultValue(),
                                      parameter.getCapabilities());
    }

    private void validateMuleVersion(Declaration declaration)
    {
        try
        {
            new MuleVersion(declaration.getVersion());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException(String.format("Invalid version %s for capability '%s'", declaration.getVersion(), declaration.getName()));
        }
    }

    private void applyPostProcessors(DescribingContext describingContext)
    {
        for (DescriberPostProcessor postProcessor : postProcessors)
        {
            postProcessor.postProcess(describingContext);
        }
    }

    private List<DescriberPostProcessor> searchPostProcessors(ServiceRegistry serviceRegistry)
    {
        return ImmutableList.<DescriberPostProcessor>builder()
                .addAll(serviceRegistry.lookupProviders(DescriberPostProcessor.class, getClass().getClassLoader()))
                .build();
    }
}
