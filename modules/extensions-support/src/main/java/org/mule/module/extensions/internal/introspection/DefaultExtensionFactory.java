/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mule.module.extensions.internal.util.MuleExtensionUtils.alphaSortDescribedList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.extensions.introspection.Configuration;
import org.mule.extensions.introspection.DescribingContext;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.ExtensionFactory;
import org.mule.extensions.introspection.Operation;
import org.mule.extensions.introspection.Parameter;
import org.mule.extensions.introspection.declaration.ConfigurationDeclaration;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.extensions.introspection.declaration.Declaration;
import org.mule.extensions.introspection.declaration.OperationDeclaration;
import org.mule.extensions.introspection.declaration.ParameterDeclaration;
import org.mule.extensions.introspection.spi.DescriberPostProcessor;
import org.mule.module.extensions.internal.ImmutableDescribingContext;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ExtensionFactory}
 *
 * @since 3.7.0
 */
public final class DefaultExtensionFactory implements ExtensionFactory
{

    private final List<DescriberPostProcessor> postProcessors;

    public DefaultExtensionFactory(ServiceRegistry serviceRegistry)
    {
        postProcessors = searchPostProcessors(serviceRegistry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Extension createFrom(Construct construct)
    {
        applyPostProcessors(construct);
        return toExtension(construct.getRootConstruct().getDeclaration());
    }


    private Extension toExtension(Declaration declaration)
    {
        validateMuleVersion(declaration);
        return new ImmutableExtension(declaration.getName(),
                                      declaration.getDescription(),
                                      declaration.getVersion(),
                                      sortConfigurations(toConfigurations(declaration.getConfigurations())),
                                      alphaSortDescribedList(toOperations(declaration.getOperations())),
                                      declaration.getCapabilities());
    }

    private List<Configuration> sortConfigurations(List<Configuration> configurations)
    {
        List<Configuration> sorted = new ArrayList<>(configurations.size());

        // first one is kept as default while the rest are alpha sorted
        sorted.add(configurations.get(0));

        if (configurations.size() > 1)
        {
            sorted.addAll(alphaSortDescribedList(configurations.subList(1, configurations.size())));
        }

        return sorted;
    }


    private List<Configuration> toConfigurations(List<ConfigurationDeclaration> declarations)
    {
        checkArgument(!declarations.isEmpty(), "A extension must have at least one configuration");

        List<Configuration> configurations = new ArrayList<>(declarations.size());
        for (ConfigurationDeclaration declaration : declarations)
        {
            configurations.add(toConfiguration(declaration));
        }

        return configurations;
    }

    private Configuration toConfiguration(ConfigurationDeclaration declaration)
    {
        return new ImmutableConfiguration(declaration.getName(),
                                          declaration.getDescription(),
                                          declaration.getConfigurationInstantiator(),
                                          toConfigParameters(declaration.getParameters()),
                                          declaration.getCapabilities());
    }

    private List<Operation> toOperations(List<OperationDeclaration> declarations)
    {
        if (declarations.isEmpty())
        {
            return ImmutableList.of();
        }

        List<Operation> operations = new ArrayList<>(declarations.size());
        for (OperationDeclaration declaration : declarations)
        {
            operations.add(toOperation(declaration));
        }

        return operations;
    }

    private Operation toOperation(OperationDeclaration declaration)
    {
        List<Parameter> parameters = toOperationParameters(declaration.getParameters());
        return new ImmutableOperation(declaration.getName(),
                                      declaration.getDescription(),
                                      declaration.getImplementation(),
                                      parameters,
                                      declaration.getCapabilities());
    }

    private List<Parameter> toConfigParameters(List<ParameterDeclaration> declarations)
    {

        List<Parameter> parameters = toParameters(declarations);
        alphaSortDescribedList(parameters);

        return parameters;
    }

    private List<Parameter> toOperationParameters(List<ParameterDeclaration> declarations)
    {
        return toParameters(declarations);
    }

    private List<Parameter> toParameters(List<ParameterDeclaration> declarations)
    {
        if (declarations.isEmpty())
        {
            return ImmutableList.of();
        }

        List<Parameter> parameters = new ArrayList<>(declarations.size());
        for (ParameterDeclaration declaration : declarations)
        {
            parameters.add(toParameter(declaration));
        }

        return parameters;
    }

    private Parameter toParameter(ParameterDeclaration parameter)
    {
        return new ImmutableParameter(parameter.getName(),
                                      parameter.getDescription(),
                                      parameter.getType(),
                                      parameter.isRequired(),
                                      parameter.isDynamic(),
                                      parameter.getDefaultValue(),
                                      parameter.getCapabilities());
    }

    private void validateMuleVersion(Declaration declaration)
    {
        // make sure version is valid
        try
        {
            new MuleVersion(declaration.getVersion());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException(String.format("Invalid version %s for capability %s", declaration.getVersion(), declaration.getName()));
        }
    }

    private void applyPostProcessors(Construct construct)
    {
        final DescribingContext context = new ImmutableDescribingContext(construct.getRootConstruct());

        for (DescriberPostProcessor postProcessor : postProcessors)
        {
            postProcessor.postProcess(context);
        }
    }

    private List<DescriberPostProcessor> searchPostProcessors(ServiceRegistry serviceRegistry)
    {
        return ImmutableList.<DescriberPostProcessor>builder()
                .addAll(serviceRegistry.lookupProviders(DescriberPostProcessor.class, getClass().getClassLoader()))
                .build();
    }
}
