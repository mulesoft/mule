/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.dsl.DslResolvingContext;
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
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeHandlerManagerFactory;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

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
public class ExtensionModelHelper {

  private final Set<ExtensionModel> extensionsModels;
  private final DslResolvingContext dslResolvingCtx;
  private final Cache<ComponentIdentifier, Optional<? extends org.mule.runtime.api.meta.model.ComponentModel>> extensionComponentModelByComponentIdentifier =
      Caffeine.newBuilder().build();
  private final Cache<ComponentIdentifier, Optional<? extends ConnectionProviderModel>> extensionConnectionProviderModelByComponentIdentifier =
      Caffeine.newBuilder().build();
  private final Cache<ComponentIdentifier, Optional<? extends ConfigurationModel>> extensionConfigurationModelByComponentIdentifier =
      Caffeine.newBuilder().build();
  private final LoadingCache<ExtensionModel, DslSyntaxResolver> dslSyntaxResolversByExtension;

  private final JavaTypeLoader javaTypeLoader = new JavaTypeLoader(ExtensionModelHelper.class.getClassLoader(),
                                                                   new ExtensionsTypeHandlerManagerFactory());

  /**
   * @param extensionModels the set of {@link ExtensionModel}s to work with. Usually this is the set of models configured within a
   *        mule artifact.
   */
  public ExtensionModelHelper(Set<ExtensionModel> extensionModels) {
    this.extensionsModels = extensionModels;
    this.dslResolvingCtx = DslResolvingContext.getDefault(extensionsModels);
    this.dslSyntaxResolversByExtension =
        Caffeine.newBuilder().build(key -> DslSyntaxResolver.getDefault(key, dslResolvingCtx));
  }

  /**
   * Find a {@link DslElementModel} for a given {@link ComponentModel}
   *
   * @param componentIdentifier the identifier to use for the search.
   * @return the {@link DslElementModel} associated with the configuration or an {@link Optional#empty()} if there isn't one.
   */
  public ComponentType findComponentType(ComponentIdentifier componentIdentifier) {
    return findComponentModel(componentIdentifier)
        .map(extensionComponentModel -> findComponentType(extensionComponentModel))
        .orElse(UNKNOWN);
  }

  public ComponentType findComponentType(org.mule.runtime.api.meta.model.ComponentModel extensionComponentModel) {
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
  }

  /**
   * Finds a {@link org.mule.runtime.api.meta.model.ComponentModel} within the provided set of {@link ExtensionModel}s by a
   * {@link ComponentIdentifier}.
   *
   * @param componentIdentifier the identifier to use for the search.
   * @return the found {@link org.mule.runtime.api.meta.model.ComponentModel} or {@link Optional#empty()} if it couldn't be found.
   */
  public Optional<? extends org.mule.runtime.api.meta.model.ComponentModel> findComponentModel(ComponentIdentifier componentId) {
    return extensionComponentModelByComponentIdentifier.get(componentId, componentIdentifier -> {
      return lookupExtensionModelFor(componentIdentifier)
          .flatMap(extensionModel -> {
            AtomicReference<org.mule.runtime.api.meta.model.ComponentModel> modelRef = new AtomicReference<>();

            new ExtensionWalker() {

              @Override
              protected void onOperation(HasOperationModels owner, OperationModel model) {
                if (dslSyntaxResolversByExtension.get(extensionModel).resolve(model).getElementName()
                    .equals(componentIdentifier.getName())) {
                  modelRef.set(model);
                }
              }

              @Override
              protected void onSource(HasSourceModels owner, SourceModel model) {
                if (dslSyntaxResolversByExtension.get(extensionModel).resolve(model).getElementName()
                    .equals(componentIdentifier.getName())) {
                  modelRef.set(model);
                }
              }

              @Override
              protected void onConstruct(HasConstructModels owner, ConstructModel model) {
                if (dslSyntaxResolversByExtension.get(extensionModel).resolve(model).getElementName()
                    .equals(componentIdentifier.getName())) {
                  modelRef.set(model);
                }
              }

            }.walk(extensionModel);

            return ofNullable(modelRef.get());
          });
    });
  }

  /**
   * Finds a {@link org.mule.runtime.api.meta.model.ConnectionProviderModel} within the provided set of {@link ExtensionModel}s by
   * a {@link ComponentIdentifier}.
   *
   * @param componentIdentifier the identifier to use for the search.
   * @return the found {@link org.mule.runtime.api.meta.model.ConnectionProviderModel} or {@link Optional#empty()} if it couldn't
   *         be found.
   */
  public Optional<? extends ConnectionProviderModel> findConnectionProviderModel(ComponentIdentifier componentId) {

    return extensionConnectionProviderModelByComponentIdentifier.get(componentId, componentIdentifier -> {
      return lookupExtensionModelFor(componentIdentifier)
          .flatMap(currentExtension -> {
            AtomicReference<ConnectionProviderModel> modelRef = new AtomicReference<>();

            new ExtensionWalker() {

              @Override
              protected void onConnectionProvider(org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels owner,
                                                  ConnectionProviderModel model) {
                if (dslSyntaxResolversByExtension.get(currentExtension).resolve(model).getElementName()
                    .equals(componentIdentifier.getName())) {
                  modelRef.set(model);
                }
              };

            }.walk(currentExtension);

            return ofNullable(modelRef.get());
          });
    });
  }

  /**
   * Finds a {@link org.mule.runtime.api.meta.model.ConfigurationModel} within the provided set of {@link ExtensionModel}s by a
   * {@link ComponentIdentifier}.
   *
   * @param componentIdentifier the identifier to use for the search.
   * @return the found {@link org.mule.runtime.api.meta.model.ConfigurationModel} or {@link Optional#empty()} if it couldn't be
   *         found.
   */
  public Optional<? extends ConfigurationModel> findConfigurationModel(ComponentIdentifier componentId) {
    return extensionConfigurationModelByComponentIdentifier.get(componentId, componentIdentifier -> {
      return lookupExtensionModelFor(componentIdentifier)
          .flatMap(currentExtension -> {
            AtomicReference<ConfigurationModel> modelRef = new AtomicReference<>();

            new ExtensionWalker() {

              @Override
              protected void onConfiguration(ConfigurationModel model) {
                if (dslSyntaxResolversByExtension.get(currentExtension).resolve(model).getElementName()
                    .equals(componentIdentifier.getName())) {
                  modelRef.set(model);
                }
              }

            }.walk(currentExtension);

            return ofNullable(modelRef.get());
          });
    });
  }

  /**
   * Navigates the extension model for the provided {@code componentIdentifier} and calls the corresponding method on the provided
   * {@code delegate} when found.
   *
   * @param componentIdentifier the identifier to use for the search.
   * @param delegate the callback to execute on the found model.
   */
  public void walkToComponent(ComponentIdentifier componentIdentifier, ExtensionWalkerModelDelegate delegate) {
    lookupExtensionModelFor(componentIdentifier)
        .ifPresent(currentExtension -> {
          new ExtensionWalker() {

            final DslSyntaxResolver dslSyntaxResolver = dslSyntaxResolversByExtension.get(currentExtension);

            @Override
            protected void onConfiguration(ConfigurationModel model) {
              if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                delegate.onConfiguration(model);
              }
            }

            @Override
            protected void onConnectionProvider(org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels owner,
                                                ConnectionProviderModel model) {
              if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                delegate.onConnectionProvider(model);
              }
            };

            @Override
            protected void onOperation(HasOperationModels owner, OperationModel model) {
              if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                delegate.onOperation(model);
              }
            }

            @Override
            protected void onSource(HasSourceModels owner, SourceModel model) {
              if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                delegate.onSource(model);
              }
            }

            @Override
            protected void onConstruct(HasConstructModels owner, ConstructModel model) {
              if (dslSyntaxResolver.resolve(model).getElementName().equals(componentIdentifier.getName())) {
                delegate.onConstruct(model);
              }
            }

          }.walk(currentExtension);
        });
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

  private Optional<ExtensionModel> lookupExtensionModelFor(ComponentIdentifier componentIdentifier) {
    return extensionsModels.stream()
        .filter(e -> e.getXmlDslModel().getPrefix().equals(componentIdentifier.getNamespace()))
        .findFirst();
  }

  /**
   * This interface is used along with an ExtensionWalker. The {@link ExtensionWalker} makes same validation/filter and then calls
   * the appropriate method form this interface if applicable.
   *
   * @since 4.3
   */
  public static interface ExtensionWalkerModelDelegate {

    void onConfiguration(ConfigurationModel model);

    void onConnectionProvider(ConnectionProviderModel model);

    void onOperation(OperationModel model);

    void onSource(SourceModel model);

    void onConstruct(ConstructModel model);

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
