/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static java.util.stream.Collectors.toList;
import static org.mule.api.expression.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.api.expression.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.extension.api.introspection.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.extension.api.introspection.ExpressionSupport.REQUIRED;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.alphaSortDescribedList;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import org.mule.api.registry.ServiceRegistry;
import org.mule.common.MuleVersion;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.ExtensionFactory;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.extension.api.runtime.OperationExecutorFactory;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.module.extension.internal.introspection.validation.ConfigurationModelValidator;
import org.mule.module.extension.internal.introspection.validation.ConnectionProviderModelValidator;
import org.mule.module.extension.internal.introspection.validation.ModelValidator;
import org.mule.module.extension.internal.introspection.validation.NameClashModelValidator;
import org.mule.module.extension.internal.introspection.validation.OperationReturnTypeModelValidator;
import org.mule.module.extension.internal.introspection.validation.ParameterModelValidator;
import org.mule.module.extension.internal.introspection.validation.TargetParameterModelValidator;
import org.mule.module.extension.internal.runtime.executor.OperationExecutorFactoryWrapper;
import org.mule.util.CollectionUtils;
import org.mule.util.ValueHolder;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ExtensionFactory}.
 * <p>
 * It transforms {@link Descriptor} instances into fully fledged instances of
 * {@link ImmutableExtensionModel}. Because the {@link Descriptor} is a raw, unvalidated
 * object model, this instance uses a fixed list of {@link ModelValidator} to assure
 * that the produced model is legal.
 * <p>
 * It uses a {@link ServiceRegistry} to locate instances of {@link ModelEnricher}.
 * The discovery happens when the {@link #DefaultExtensionFactory(ServiceRegistry, ClassLoader)}
 * constructor is invoked and the list of discovered instances will be used during
 * the whole duration of this instance.
 *
 * @since 3.7.0
 */
public final class DefaultExtensionFactory implements ExtensionFactory
{

    private final List<ModelEnricher> modelEnrichers;
    private final List<ModelValidator> modelValidators;

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
        modelValidators = ImmutableList.<ModelValidator>builder()
                .add(new NameClashModelValidator())
                .add(new ParameterModelValidator())
                .add(new ConnectionProviderModelValidator())
                .add(new ConfigurationModelValidator())
                .add(new OperationReturnTypeModelValidator())
                .add(new TargetParameterModelValidator())
                .build();
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
        ExtensionModel extensionModel = toExtension(descriptor.getRootDeclaration().getDeclaration());
        modelValidators.forEach(v -> v.validate(extensionModel));

        return extensionModel;
    }

    private ExtensionModel toExtension(Declaration declaration)
    {
        validateMuleVersion(declaration);
        ValueHolder<ExtensionModel> extensionModelValueHolder = new ValueHolder<>();
        ExtensionModel extensionModel = new ImmutableExtensionModel(declaration.getName(),
                                                                    declaration.getDescription(),
                                                                    declaration.getVersion(),
                                                                    declaration.getVendor(),
                                                                    sortConfigurations(toConfigurations(declaration.getConfigurations(), extensionModelValueHolder)),
                                                                    alphaSortDescribedList(toOperations(declaration.getOperations())),
                                                                    toConnectionProviders(declaration.getConnectionProviders()),
                                                                    alphaSortDescribedList(toMessageSources(declaration.getMessageSources())),
                                                                    declaration.getModelProperties(),
                                                                    declaration.getExceptionEnricherFactory());

        extensionModelValueHolder.set(extensionModel);
        return extensionModel;
    }

    private List<ConfigurationModel> sortConfigurations(List<ConfigurationModel> configurationModels)
    {
        if (CollectionUtils.isEmpty(configurationModels))
        {
            return configurationModels;
        }

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
        return declarations.stream()
                .map(declaration -> toConfiguration(declaration, extensionModelValueHolder))
                .collect(toList());
    }

    private ConfigurationModel toConfiguration(ConfigurationDeclaration declaration, ValueHolder<ExtensionModel> extensionModel)
    {
        return new ImmutableConfigurationModel(declaration.getName(),
                                               declaration.getDescription(),
                                               extensionModel::get,
                                               declaration.getConfigurationFactory(),
                                               toParameters(declaration.getParameters()),
                                               declaration.getModelProperties(),
                                               declaration.getInterceptorFactories());
    }

    private List<SourceModel> toMessageSources(List<SourceDeclaration> declarations)
    {
        return declarations.stream()
                .map(declaration -> toMessageSource(declaration))
                .collect(toList());
    }

    private SourceModel toMessageSource(SourceDeclaration declaration)
    {
        return new ImmutableSourceModel(declaration.getName(),
                                        declaration.getDescription(),
                                        toParameters(declaration.getParameters()),
                                        declaration.getReturnType(),
                                        declaration.getAttributesType(),
                                        declaration.getSourceFactory(),
                                        declaration.getModelProperties(),
                                        declaration.getInterceptorFactories());
    }

    private List<OperationModel> toOperations(List<OperationDeclaration> declarations)
    {
        return declarations.stream().map(this::toOperation).collect(toList());
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
                                           declaration.getReturnType(),
                                           declaration.getModelProperties(),
                                           declaration.getInterceptorFactories(),
                                           declaration.getExceptionEnricherFactory());
    }

    private List<ConnectionProviderModel> toConnectionProviders(List<ConnectionProviderDeclaration> declarations)
    {
        return declarations.stream().map(this::toConnectionProvider).collect(new ImmutableListCollector<>());
    }

    private ConnectionProviderModel toConnectionProvider(ConnectionProviderDeclaration declaration)
    {
        return new ImmutableConnectionProviderModel(
                declaration.getName(),
                declaration.getDescription(),
                declaration.getConfigurationType(),
                declaration.getConnectionType(),
                declaration.getFactory(),
                toParameters(declaration.getParameters()),
                declaration.getModelProperties());
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

        return declarations.stream().map(this::toParameter).collect(toList());
    }

    private ParameterModel toParameter(ParameterDeclaration parameter)
    {
        Object defaultValue = parameter.getDefaultValue();
        if (defaultValue instanceof String)
        {
            if (parameter.getExpressionSupport() == NOT_SUPPORTED && isExpression((String) defaultValue))
            {
                throw new IllegalParameterModelDefinitionException(String.format("Parameter '%s' is marked as not supporting expressions yet it contains one as a default value. Please fix this", parameter.getName()));
            }
            else if (parameter.getExpressionSupport() == REQUIRED && !isExpression((String) defaultValue))
            {
                throw new IllegalParameterModelDefinitionException(String.format("Parameter '%s' requires expressions yet it contains a constant as a default value. Please fix this", parameter.getName()));
            }
        }

        return new ImmutableParameterModel(parameter.getName(),
                                           parameter.getDescription(),
                                           parameter.getType(),
                                           parameter.isRequired(),
                                           parameter.getExpressionSupport(),
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

    private boolean isExpression(String value)
    {
        return value.startsWith(DEFAULT_EXPRESSION_PREFIX) &&
               value.endsWith(DEFAULT_EXPRESSION_POSTFIX);
    }
}
