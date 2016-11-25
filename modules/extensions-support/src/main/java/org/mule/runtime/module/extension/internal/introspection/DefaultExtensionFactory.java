/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.alphaSortDescribedList;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.model.ImmutableExtensionModel;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.model.config.ImmutableConfigurationModel;
import org.mule.runtime.extension.api.model.connection.ImmutableConnectionProviderModel;
import org.mule.runtime.extension.api.model.operation.ImmutableOperationModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableExclusiveParametersModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterGroupModel;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.api.model.source.ImmutableSourceModel;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.validation.ConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ConnectionProviderNameModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ContentParameterModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ExclusiveParameterModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ExportedTypesModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.MetadataComponentModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.NameClashModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.OperationParametersModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.OperationReturnTypeModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.ParameterModelValidator;
import org.mule.runtime.module.extension.internal.introspection.validation.SubtypesModelValidator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ExtensionFactory}.
 * <p>
 * It transforms {@link ExtensionDeclarer} instances into fully fledged instances of {@link ImmutableExtensionModel}. Because the
 * {@link ExtensionDeclarer} is a raw, unvalidated object model, this instance uses a fixed list of {@link ModelValidator} to
 * assure that the produced model is legal.
 * <p>
 * It uses a {@link ServiceRegistry} to locate instances of {@link ModelEnricher}. The discovery happens when the
 * {@link #DefaultExtensionFactory(ServiceRegistry, ClassLoader)} constructor is invoked and the list of discovered instances will
 * be used during the whole duration of this instance.
 *
 * @since 3.7.0
 */
public final class DefaultExtensionFactory implements ExtensionFactory {

  private final List<ModelEnricher> modelEnrichers;
  private final List<ModelValidator> modelValidators;

  /**
   * Creates a new instance and uses the given {@code serviceRegistry} to locate instances of {@link ModelEnricher}
   *
   * @param serviceRegistry a {@link ServiceRegistry}
   * @param classLoader     the {@link ClassLoader} on which the {@code serviceRegistry} will search into
   */
  public DefaultExtensionFactory(ServiceRegistry serviceRegistry, ClassLoader classLoader) {
    modelEnrichers = ImmutableList.copyOf(serviceRegistry.lookupProviders(ModelEnricher.class, classLoader));
    modelValidators = ImmutableList.<ModelValidator>builder()
        .add(new SubtypesModelValidator())
        .add(new NameClashModelValidator())
        .add(new ParameterModelValidator())
        .add(new ExportedTypesModelValidator())
        .add(new ConnectionProviderModelValidator())
        .add(new ConfigurationModelValidator())
        .add(new OperationReturnTypeModelValidator())
        .add(new OperationParametersModelValidator())
        .add(new MetadataComponentModelValidator())
        .add(new ExclusiveParameterModelValidator())
        .add(new ConnectionProviderNameModelValidator())
        .add(new ContentParameterModelValidator())
        .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionModel createFrom(ExtensionDeclarer declarer, DescribingContext describingContext) {
    enrichModel(describingContext);
    ExtensionModel extensionModel = new FactoryDelegate().toExtension(declarer.getDeclaration());
    modelValidators.forEach(v -> v.validate(extensionModel));

    return extensionModel;
  }

  private void validateMuleVersion(ExtensionDeclaration extensionDeclaration) {
    try {
      new MuleVersion(extensionDeclaration.getVersion());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(format("Invalid version '%s' for extension '%s'",
                                                extensionDeclaration.getVersion(), extensionDeclaration.getName()));
    }
  }

  private void enrichModel(DescribingContext describingContext) {
    modelEnrichers.forEach(enricher -> enricher.enrich(describingContext));
  }

  private boolean isExpression(String value) {
    return value.startsWith(DEFAULT_EXPRESSION_PREFIX) && value.endsWith(DEFAULT_EXPRESSION_POSTFIX);
  }

  private class FactoryDelegate {

    private Cache<ParameterizedDeclaration, ParameterizedModel> modelCache = CacheBuilder.newBuilder().build();

    private ExtensionModel toExtension(ExtensionDeclaration extensionDeclaration) {
      validateMuleVersion(extensionDeclaration);
      ExtensionModel extensionModel =
          new ImmutableExtensionModel(extensionDeclaration.getName(), extensionDeclaration.getDescription(),
                                      extensionDeclaration.getVersion(), extensionDeclaration.getVendor(),
                                      extensionDeclaration.getCategory(), extensionDeclaration.getMinMuleVersion(),
                                      sortConfigurations(toConfigurations(extensionDeclaration.getConfigurations())),
                                      toOperations(extensionDeclaration.getOperations()),
                                      toConnectionProviders(extensionDeclaration.getConnectionProviders()),
                                      toMessageSources(extensionDeclaration.getMessageSources()),
                                      extensionDeclaration.getDisplayModel(),
                                      extensionDeclaration.getXmlDslModel(),
                                      extensionDeclaration.getSubTypes(),
                                      extensionDeclaration.getTypes(),
                                      extensionDeclaration.getImportedTypes(),
                                      extensionDeclaration.getModelProperties());

      return extensionModel;
    }

    private List<ConfigurationModel> sortConfigurations(List<ConfigurationModel> configurationModels) {
      if (CollectionUtils.isEmpty(configurationModels)) {
        return configurationModels;
      }

      return alphaSortDescribedList(configurationModels);
    }


    private List<ConfigurationModel> toConfigurations(List<ConfigurationDeclaration> declarations) {
      return declarations.stream().map(this::toConfiguration).collect(toList());
    }

    private <T extends ParameterizedModel> T fromCache(ParameterizedDeclaration declaration,
                                                       Supplier<ParameterizedModel> supplier) {
      try {
        return (T) modelCache.get(declaration, supplier::get);
      } catch (UncheckedExecutionException e) {
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        }
        throw e;
      } catch (ExecutionException e) {
        throw new MuleRuntimeException(e);
      }
    }

    private ConfigurationModel toConfiguration(ConfigurationDeclaration declaration) {
      return fromCache(declaration,
                       () -> new ImmutableConfigurationModel(declaration.getName(),
                                                             declaration.getDescription(),
                                                             toParameterGroups(declaration.getParameterGroups()),
                                                             toOperations(declaration.getOperations()),
                                                             toConnectionProviders(declaration.getConnectionProviders()),
                                                             toMessageSources(declaration.getMessageSources()),
                                                             declaration.getDisplayModel(),
                                                             declaration.getModelProperties()));
    }

    private List<SourceModel> toMessageSources(List<SourceDeclaration> declarations) {
      return alphaSortDescribedList(declarations.stream().map(this::toMessageSource).collect(toList()));
    }

    private SourceModel toMessageSource(SourceDeclaration declaration) {
      return fromCache(declaration,
                       () -> new ImmutableSourceModel(declaration.getName(), declaration.getDescription(),
                                                      declaration.hasResponse(),
                                                      toParameterGroups(declaration.getParameterGroups()),
                                                      toOutputModel(declaration.getOutput()),
                                                      toOutputModel(declaration.getOutputAttributes()),
                                                      declaration.getDisplayModel(),
                                                      declaration.getModelProperties()));
    }

    private List<OperationModel> toOperations(List<OperationDeclaration> declarations) {
      return alphaSortDescribedList(declarations.stream().map(this::toOperation).collect(toList()));
    }

    private OperationModel toOperation(OperationDeclaration declaration) {
      return fromCache(declaration, () -> new ImmutableOperationModel(declaration.getName(),
                                                                      declaration.getDescription(),
                                                                      toParameterGroups(declaration.getParameterGroups()),
                                                                      toOutputModel(declaration.getOutput()),
                                                                      toOutputModel(declaration.getOutputAttributes()),
                                                                      declaration.getDisplayModel(),
                                                                      declaration.getModelProperties()));
    }

    private List<ConnectionProviderModel> toConnectionProviders(List<ConnectionProviderDeclaration> declarations) {
      return declarations.stream().map(this::toConnectionProvider).collect(new ImmutableListCollector<>());
    }

    private OutputModel toOutputModel(OutputDeclaration declaration) {
      return declaration != null
          ? new ImmutableOutputModel(declaration.getDescription(), declaration.getType(), declaration.hasDynamicType(),
                                     declaration.getModelProperties())
          : new ImmutableOutputModel(EMPTY, create(JAVA).voidType().build(), false, emptySet());
    }

    private ConnectionProviderModel toConnectionProvider(ConnectionProviderDeclaration declaration) {
      return fromCache(declaration,
                       () -> new ImmutableConnectionProviderModel(declaration.getName(),
                                                                  declaration.getDescription(),
                                                                  toParameterGroups(declaration.getParameterGroups()),
                                                                  declaration.getConnectionManagementType(),
                                                                  declaration.getDisplayModel(),
                                                                  declaration.getModelProperties()));
    }

    private List<ParameterGroupModel> toParameterGroups(List<ParameterGroupDeclaration> declarations) {
      if (declarations.isEmpty()) {
        return ImmutableList.of();
      }

      return declarations.stream().map(this::toParameterGroup).collect(toList());
    }

    private ParameterGroupModel toParameterGroup(ParameterGroupDeclaration declaration) {
      return new ImmutableParameterGroupModel(declaration.getName(),
                                              declaration.getDescription(),
                                              toParameters(declaration.getParameters()),
                                              toExclusiveParametersModels(declaration),
                                              declaration.getDisplayModel(),
                                              declaration.getLayoutModel(),
                                              declaration.getModelProperties());
    }

    private List<ExclusiveParametersModel> toExclusiveParametersModels(ParameterGroupDeclaration groupDeclaration) {
      return groupDeclaration.getExclusiveParameters().stream()
          .map(exclusive -> new ImmutableExclusiveParametersModel(exclusive.getParameterNames(), exclusive.isRequiresOne()))
          .collect(new ImmutableListCollector<>());
    }

    private List<ParameterModel> toParameters(List<ParameterDeclaration> declarations) {
      if (declarations.isEmpty()) {
        return ImmutableList.of();
      }

      return declarations.stream().map(this::toParameter).collect(toList());
    }

    private ParameterModel toParameter(ParameterDeclaration parameter) {
      Object defaultValue = parameter.getDefaultValue();
      if (defaultValue instanceof String) {
        if (parameter.getExpressionSupport() == NOT_SUPPORTED && isExpression((String) defaultValue)) {
          throw new IllegalParameterModelDefinitionException(
                                                             format("Parameter '%s' is marked as not supporting expressions yet it"
                                                                 + " contains one as a default value. Please fix this",
                                                                    parameter.getName()));
        } else if (parameter.getExpressionSupport() == REQUIRED && !isExpression((String) defaultValue)) {
          throw new IllegalParameterModelDefinitionException(format("Parameter '%s' requires expressions yet it "
              + "contains a constant as a default value. Please fix this",
                                                                    parameter.getName()));
        }
      }

      return new ImmutableParameterModel(parameter.getName(),
                                         parameter.getDescription(),
                                         parameter.getType(),
                                         parameter.hasDynamicType(),
                                         parameter.isRequired(),
                                         parameter.getExpressionSupport(),
                                         parameter.getDefaultValue(),
                                         parameter.getRole(),
                                         parameter.getDslModel(),
                                         parameter.getDisplayModel(),
                                         parameter.getLayoutModel(),
                                         parameter.getModelProperties());
    }
  }
}
