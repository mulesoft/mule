/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.core.api.expression.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.expression.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.alphaSortDescribedList;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import org.mule.common.MuleVersion;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ImmutableExtensionModel;
import org.mule.runtime.extension.api.introspection.ImmutableOutputModel;
import org.mule.runtime.extension.api.introspection.ImmutableRuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.OutputModel;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.ImmutableRuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.ImmutableRuntimeConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OutputDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.operation.ImmutableRuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.source.ImmutableRuntimeSourceModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.validation.ConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ExclusiveParameterModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.MetadataComponentModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.NameClashModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.OperationParametersModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.OperationReturnTypeModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ParameterModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.SubtypesModelValidator;
import org.mule.runtime.module.extension.internal.runtime.executor.OperationExecutorFactoryWrapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ExtensionFactory}.
 * <p>
 * It transforms {@link ExtensionDeclarer} instances into fully fledged instances of
 * {@link ImmutableExtensionModel}. Because the {@link ExtensionDeclarer} is a raw, unvalidated
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
                .add(new SubtypesModelValidator())
                .add(new NameClashModelValidator())
                .add(new ParameterModelValidator())
                .add(new ConnectionProviderModelValidator())
                .add(new ConfigurationModelValidator())
                .add(new OperationReturnTypeModelValidator())
                .add(new OperationParametersModelValidator())
                .add(new MetadataComponentModelValidator())
                .add(new ExclusiveParameterModelValidator())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuntimeExtensionModel createFrom(ExtensionDeclarer declarer, DescribingContext describingContext)
    {
        enrichModel(describingContext);
        RuntimeExtensionModel extensionModel = new FactoryDelegate().toExtension(declarer.getDeclaration());
        modelValidators.forEach(v -> v.validate(extensionModel));

        return extensionModel;
    }

    private void validateMuleVersion(ExtensionDeclaration extensionDeclaration)
    {
        try
        {
            new MuleVersion(extensionDeclaration.getVersion());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException(String.format("Invalid version '%s' for extension '%s'", extensionDeclaration.getVersion(), extensionDeclaration.getName()));
        }
    }

    private void enrichModel(DescribingContext describingContext)
    {
        modelEnrichers.forEach(enricher -> enricher.enrich(describingContext));
    }

    private boolean isExpression(String value)
    {
        return value.startsWith(DEFAULT_EXPRESSION_PREFIX) && value.endsWith(DEFAULT_EXPRESSION_POSTFIX);
    }

    private class FactoryDelegate
    {

        private Cache<ParameterizedDeclaration, ParameterizedModel> modelCache = CacheBuilder.newBuilder().build();

        private RuntimeExtensionModel toExtension(ExtensionDeclaration extensionDeclaration)
        {
            validateMuleVersion(extensionDeclaration);
            ValueHolder<RuntimeExtensionModel> extensionModelValueHolder = new ValueHolder<>();
            RuntimeExtensionModel extensionModel = new ImmutableRuntimeExtensionModel(extensionDeclaration.getName(),
                                                                                      extensionDeclaration.getDescription(),
                                                                                      extensionDeclaration.getVersion(),
                                                                                      extensionDeclaration.getVendor(),
                                                                                      extensionDeclaration.getCategory(),
                                                                                      extensionDeclaration.getMinMuleVersion(),
                                                                                      sortConfigurations(toConfigurations(extensionDeclaration.getConfigurations(), extensionModelValueHolder)),
                                                                                      toOperations(extensionDeclaration.getOperations()),
                                                                                      toConnectionProviders(extensionDeclaration.getConnectionProviders()),
                                                                                      toMessageSources(extensionDeclaration.getMessageSources()),
                                                                                      extensionDeclaration.getModelProperties(),
                                                                                      extensionDeclaration.getExceptionEnricherFactory());

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


        private List<ConfigurationModel> toConfigurations(List<ConfigurationDeclaration> declarations, ValueHolder<RuntimeExtensionModel> extensionModelValueHolder)
        {
            return declarations.stream()
                    .map(declaration -> toConfiguration(declaration, extensionModelValueHolder))
                    .collect(toList());
        }

        private <T extends ParameterizedModel> T fromCache(ParameterizedDeclaration declaration, Supplier<ParameterizedModel> supplier)
        {
            try
            {
                return (T) modelCache.get(declaration, supplier::get);
            }
            catch (UncheckedExecutionException e)
            {
                if (e.getCause() instanceof RuntimeException)
                {
                    throw (RuntimeException) e.getCause();
                }
                throw e;
            }
            catch (ExecutionException e)
            {
                throw new MuleRuntimeException(e);
            }
        }

        private ConfigurationModel toConfiguration(ConfigurationDeclaration declaration, ValueHolder<RuntimeExtensionModel> extensionModel)
        {
            return fromCache(declaration, () ->
                    new ImmutableRuntimeConfigurationModel(declaration.getName(),
                                                           declaration.getDescription(),
                                                           extensionModel::get,
                                                           declaration.getConfigurationFactory(),
                                                           toParameters(declaration.getParameters()),
                                                           toOperations(declaration.getOperations()),
                                                           toConnectionProviders(declaration.getConnectionProviders()),
                                                           toMessageSources(declaration.getMessageSources()),
                                                           declaration.getModelProperties(),
                                                           declaration.getInterceptorFactories())
            );
        }

        private List<SourceModel> toMessageSources(List<SourceDeclaration> declarations)
        {
            return alphaSortDescribedList(declarations.stream().map(this::toMessageSource).collect(toList()));
        }

        private RuntimeSourceModel toMessageSource(SourceDeclaration declaration)
        {
            return fromCache(declaration, () ->
                    new ImmutableRuntimeSourceModel(declaration.getName(),
                                                    declaration.getDescription(),
                                                    toParameters(declaration.getParameters()),
                                                    toOutputModel(declaration.getOutput()),
                                                    toOutputModel(declaration.getOutputAttributes()),
                                                    declaration.getSourceFactory(),
                                                    declaration.getModelProperties(),
                                                    declaration.getInterceptorFactories(),
                                                    declaration.getExceptionEnricherFactory(),
                                                    declaration.getMetadataResolverFactory())
            );
        }

        private List<OperationModel> toOperations(List<OperationDeclaration> declarations)
        {
            return alphaSortDescribedList(declarations.stream().map(this::toOperation).collect(toList()));
        }

        private RuntimeOperationModel toOperation(OperationDeclaration declaration)
        {
            return fromCache(declaration, () -> {
                List<ParameterModel> parameterModels = toOperationParameters(declaration.getParameters());

                List<Interceptor> interceptors = createInterceptors(declaration.getInterceptorFactories());
                OperationExecutorFactory executorFactory = new OperationExecutorFactoryWrapper(declaration.getExecutorFactory(), interceptors);

                return new ImmutableRuntimeOperationModel(declaration.getName(),
                                                          declaration.getDescription(),
                                                          executorFactory,
                                                          parameterModels,
                                                          toOutputModel(declaration.getOutput()),
                                                          toOutputModel(declaration.getOutputAttributes()),
                                                          declaration.getModelProperties(),
                                                          declaration.getInterceptorFactories(),
                                                          declaration.getExceptionEnricherFactory(),
                                                          declaration.getMetadataResolverFactory());
            });
        }

        private List<ConnectionProviderModel> toConnectionProviders(List<ConnectionProviderDeclaration> declarations)
        {
            return declarations.stream().map(this::toConnectionProvider).collect(new ImmutableListCollector<>());
        }

        private OutputModel toOutputModel(OutputDeclaration declaration)
        {
            return declaration != null ? new ImmutableOutputModel(declaration.getDescription(),
                                                                  declaration.getType(), declaration.hasDynamicType(),
                                                                  declaration.getModelProperties())
                                       : new ImmutableOutputModel(EMPTY, create(JAVA).nullType().build(), false, emptySet());
        }

        private RuntimeConnectionProviderModel toConnectionProvider(ConnectionProviderDeclaration declaration)
        {
            return fromCache(declaration, () ->
                    new ImmutableRuntimeConnectionProviderModel(
                            declaration.getName(),
                            declaration.getDescription(),
                            declaration.getConnectionType(),
                            declaration.getFactory(),
                            toParameters(declaration.getParameters()),
                            declaration.getConnectionManagementType(),
                            declaration.getModelProperties())
            );
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
                                               parameter.hasDynamicType(),
                                               parameter.isRequired(),
                                               parameter.getExpressionSupport(),
                                               parameter.getDefaultValue(),
                                               parameter.getModelProperties());

        }
    }
}
