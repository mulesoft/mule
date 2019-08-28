/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.util.NameUtils.COMPONENT_NAME_SEPARATOR;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.ORIGINAL_IDENTIFIER;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModelVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeHandlerManagerFactory;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

/**
 * Helper class to work with a set of {@link ExtensionModel}s
 * <p/>
 * Contains a cache for searches within the extension models so we avoid processing each extension model twice.
 * <p/>
 * It's recommended that the application only has one instance of this class to avoid processing the extension models several
 * times.
 * <p>
 * since 4.0
 */
// TODO MULE-15143 Support a lightweight implementation of DslElementModelFactory to only identify the model from
// ComponentIdentifier
public class ExtensionModelHelper {

  private final Set<ExtensionModel> extensionsModels;
  private final DslResolvingContext dslResolvingCtx;
  private final Cache<ComponentIdentifier, Optional<? extends org.mule.runtime.api.meta.model.ComponentModel>> extensionComponentModelByComponentIdentifier =
      CacheBuilder.newBuilder().build();
  private final Cache<ComponentIdentifier, Optional<? extends ConnectionProviderModel>> extensionConnectionProviderModelByComponentIdentifier =
      CacheBuilder.newBuilder().build();
  private final Cache<ComponentIdentifier, Optional<? extends ConfigurationModel>> extensionConfigurationModelByComponentIdentifier =
      CacheBuilder.newBuilder().build();
  private final Cache<ComponentIdentifier, Optional<NestableElementModel>> extensionNestableElementModelByComponentIdentifier =
      CacheBuilder.newBuilder().build();

  private final JavaTypeLoader javaTypeLoader = new JavaTypeLoader(ExtensionModelHelper.class.getClassLoader(),
                                                                   new ExtensionsTypeHandlerManagerFactory());

  /**
   * @param extensionModels the set of {@link ExtensionModel}s to work with. Usually this is the set of models configured within a
   *        mule artifact.
   */
  public ExtensionModelHelper(Set<ExtensionModel> extensionModels) {
    this.extensionsModels = extensionModels;
    this.dslResolvingCtx = DslResolvingContext.getDefault(extensionsModels);
  }

  /**
   * Find a {@link DslElementModel} for a given {@link ComponentModel}
   *
   * @param componentModel the component model from the configuration.
   * @return the {@link DslElementModel} associated with the configuration or an {@link Optional#empty()} if there isn't one.
   */
  public TypedComponentIdentifier.ComponentType findComponentType(ComponentModel componentModel) {
    ComponentIdentifier componentId = componentModel.getMetadata().getParserAttributes().containsKey(ORIGINAL_IDENTIFIER)
        ? (ComponentIdentifier) componentModel.getMetadata().getParserAttributes().get(ORIGINAL_IDENTIFIER)
        : componentModel.getIdentifier();
    Optional<? extends org.mule.runtime.api.meta.model.ComponentModel> extensionComponentModelOptional =
        findComponentModel(componentId);

    return extensionComponentModelOptional.map(extensionComponentModel -> {
      Reference<TypedComponentIdentifier.ComponentType> componentTypeReference = new Reference<>();
      extensionComponentModel.accept(new ComponentModelVisitor() {

        @Override
        public void visit(OperationModel model) {
          componentTypeReference.set(OPERATION);
        }

        @Override
        public void visit(SourceModel model) {
          componentTypeReference.set(SOURCE);
        }

        @Override
        public void visit(ConstructModel model) {
          if (model.getStereotype().equals(MuleStereotypes.ERROR_HANDLER)) {
            componentTypeReference.set(ERROR_HANDLER);
            return;
          }
          if (model.getStereotype().equals(MuleStereotypes.FLOW)) {
            componentTypeReference.set(FLOW);
            return;
          }
          NestedComponentVisitor nestedComponentVisitor = new NestedComponentVisitor(componentTypeReference);
          for (NestableElementModel nestableElementModel : model.getNestedComponents()) {
            nestableElementModel.accept(nestedComponentVisitor);
            if (componentTypeReference.get() != null) {
              return;
            }
          }
        }
      });
      return componentTypeReference.get() == null ? UNKNOWN : componentTypeReference.get();
    }).orElseGet(() -> {
      // If there was no ComponentModel found, search for nestable elements, we might be talking about a ROUTE and we need to
      // return it's ComponentType as well
      Optional<? extends NestableElementModel> nestableElementModelOptional = findNestableElementModel(componentId);
      return nestableElementModelOptional.map(nestableElementModel -> {
        Reference<TypedComponentIdentifier.ComponentType> componentTypeReference = new Reference<>();
        nestableElementModel.accept(new IsRouteVisitor(componentTypeReference));
        return componentTypeReference.get() == null ? UNKNOWN : componentTypeReference.get();
      }).orElse(UNKNOWN);
    });
  }

  /**
   * Finds a {@link org.mule.runtime.api.meta.model.ComponentModel} within the provided set of {@link ExtensionModel}s by a
   * {@link ComponentIdentifier}.
   *
   * @param componentIdentifier the identifier to use for the search.
   * @return the found {@link org.mule.runtime.api.meta.model.ComponentModel} or {@link Optional#empty()} if it couldn't be found.
   */
  public Optional<? extends org.mule.runtime.api.meta.model.ComponentModel> findComponentModel(ComponentIdentifier componentIdentifier) {
    try {
      return extensionComponentModelByComponentIdentifier.get(componentIdentifier, () -> {
        return lookupExtensionModelFor(componentIdentifier).flatMap(extensionModel -> {
          List<HasOperationModels> operationModelsProviders = ImmutableList.<HasOperationModels>builder()
              .add(extensionModel).addAll(extensionModel.getConfigurationModels()).build();
          List<HasSourceModels> sourceModelsProviders = ImmutableList.<HasSourceModels>builder()
              .add(extensionModel).addAll(extensionModel.getConfigurationModels()).build();
          List<HasConstructModels> constructModelsProviders = singletonList(extensionModel);

          String componentName = toCamelCase(componentIdentifier.getName(), COMPONENT_NAME_SEPARATOR);
          Stream<Supplier<Optional<? extends org.mule.runtime.api.meta.model.ComponentModel>>> stream =
              of(() -> resolveModel(operationModelsProviders, sourceModelsProviders, constructModelsProviders, componentName),
                 () -> resolveModel(operationModelsProviders, sourceModelsProviders, constructModelsProviders,
                                    componentIdentifier.getName()),
                 () -> resolveModel(operationModelsProviders, sourceModelsProviders, constructModelsProviders,
                                    capitalize(componentName)));

          return stream
              .map(Supplier::get)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .findFirst();
        });
      });
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public Optional<? extends ConnectionProviderModel> findConnectionProviderModel(ComponentIdentifier componentIdentifier) {

    try {
      return extensionConnectionProviderModelByComponentIdentifier.get(componentIdentifier, () -> {
        return lookupExtensionModelFor(componentIdentifier)
            .flatMap(currentExtension -> {
              DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(currentExtension, dslResolvingCtx);

              AtomicReference<ConnectionProviderModel> modelRef = new AtomicReference<>();

              new ExtensionWalker() {

                @Override
                protected void onConnectionProvider(org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels owner,
                                                    ConnectionProviderModel model) {
                  if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                    modelRef.set(model);
                  }
                };

              }.walk(currentExtension);

              return ofNullable(modelRef.get());
            });
      });
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public Optional<? extends ConfigurationModel> findConfigurationModel(ComponentIdentifier componentIdentifier) {
    try {
      return extensionConfigurationModelByComponentIdentifier.get(componentIdentifier, () -> {
        return lookupExtensionModelFor(componentIdentifier)
            .flatMap(currentExtension -> {
              DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(currentExtension, dslResolvingCtx);

              AtomicReference<ConfigurationModel> modelRef = new AtomicReference<>();

              new ExtensionWalker() {

                @Override
                protected void onConfiguration(ConfigurationModel model) {
                  if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                    modelRef.set(model);
                  }
                }

              }.walk(currentExtension);

              return ofNullable(modelRef.get());
            });
      });
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  public Optional<? extends MetadataType> findMetadataType(Class<?> type) {
    if (type != null
        // workaround for test components with no extension model
        && !Processor.class.isAssignableFrom(type)) {
      return Optional.of(javaTypeLoader.load(type));
    } else {
      return empty();
    }
  }

  private Optional<NestableElementModel> findNestableElementModel(ComponentIdentifier componentIdentifier) {
    try {
      return extensionNestableElementModelByComponentIdentifier.get(componentIdentifier, () -> {

        return lookupExtensionModelFor(componentIdentifier).flatMap(extensionModel -> {
          String componentName = toCamelCase(componentIdentifier.getName(), COMPONENT_NAME_SEPARATOR);
          Optional<NestableElementModel> elementModelOptional = searchNestableElementModel(extensionModel, componentName);
          if (elementModelOptional.isPresent()) {
            return elementModelOptional;
          }
          return searchNestableElementModel(extensionModel, componentIdentifier.getName());
        });
      });
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Optional<ExtensionModel> lookupExtensionModelFor(ComponentIdentifier componentIdentifier) {
    return extensionsModels.stream()
        .filter(e -> e.getXmlDslModel().getPrefix().equals(componentIdentifier.getNamespace()))
        .findFirst();
  }

  private Optional<NestableElementModel> searchNestableElementModel(ExtensionModel extensionModel, String componentName) {
    Reference<NestableElementModel> reference = new Reference<>();
    IdempotentExtensionWalker walker = new IdempotentExtensionWalker() {

      @Override
      protected void onConstruct(ConstructModel model) {
        model.getNestedComponents().stream()
            .filter(nestedComponent -> nestedComponent.getName().equals(componentName))
            .findFirst()
            .ifPresent((foundComponent) -> {
              reference.set(foundComponent);
              stop();
            });
      }
    };
    walker.walk(extensionModel);
    return ofNullable(reference.get());
  }


  private Optional<? extends org.mule.runtime.api.meta.model.ComponentModel> resolveModel(List<HasOperationModels> operationModelsProviders,
                                                                                          List<HasSourceModels> sourceModelsProviders,
                                                                                          List<HasConstructModels> constructModelsProviders,
                                                                                          String componentName) {
    for (HasOperationModels operationModelsProvider : operationModelsProviders) {
      Optional<OperationModel> operationModel = operationModelsProvider.getOperationModel(componentName);
      if (operationModel.isPresent()) {
        return operationModel;
      }
    }
    for (HasSourceModels sourceModelsProvider : sourceModelsProviders) {
      Optional<SourceModel> sourceModel = sourceModelsProvider.getSourceModel(componentName);
      if (sourceModel.isPresent()) {
        return sourceModel;
      }
    }
    for (HasConstructModels constructModelsProvider : constructModelsProviders) {
      Optional<ConstructModel> constructModel = constructModelsProvider.getConstructModel(componentName);
      if (constructModel.isPresent()) {
        return constructModel;
      }
    }
    return empty();
  }

  static class IsRouteVisitor implements NestableElementModelVisitor {

    private final Reference<TypedComponentIdentifier.ComponentType> reference;

    public IsRouteVisitor(Reference<TypedComponentIdentifier.ComponentType> reference) {
      this.reference = reference;
    }

    @Override
    public void visit(NestedComponentModel component) {}

    @Override
    public void visit(NestedChainModel component) {}

    @Override
    public void visit(NestedRouteModel component) {
      reference.set(ROUTE);

    }
  }

  /**
   * Visitor of {@link ConstructModel} that determines it
   * {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType}
   */
  static class NestedComponentVisitor implements NestableElementModelVisitor {

    private final Reference<TypedComponentIdentifier.ComponentType> reference;

    public NestedComponentVisitor(Reference<TypedComponentIdentifier.ComponentType> reference) {
      this.reference = reference;
    }

    @Override
    public void visit(NestedComponentModel component) {}

    @Override
    public void visit(NestedChainModel component) {
      reference.set(SCOPE);
    }

    @Override
    public void visit(NestedRouteModel component) {
      reference.set(ROUTER);
    }
  }

}
