/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getExtensionsErrorNamespace;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used {@link ErrorTypes} and {@link Throws} in an Annotation based extension.
 *
 * @since 4.0
 */
public class ErrorsDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    LoadingCache<Class<?>, TypeWrapper> typeWrapperCache =
        CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, TypeWrapper>() {

          @Override
          public TypeWrapper load(Class<?> clazz) throws Exception {
            return new TypeWrapper(clazz);
          }
        });

    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    Optional<ImplementingTypeModelProperty> implementingType = declaration.getModelProperty(ImplementingTypeModelProperty.class);
    String extensionNamespace = getExtensionsErrorNamespace(declaration);
    ErrorsModelFactory errorModelDescriber = new ErrorsModelFactory(extensionNamespace);
    errorModelDescriber.getErrorModels().forEach(declaration::addErrorModel);

    if (implementingType.isPresent()) {

      ExtensionElement extensionElement = new ExtensionTypeWrapper<>(implementingType.get().getType());
      Optional<ErrorTypes> errorAnnotation = extensionElement.getAnnotation(ErrorTypes.class);
      List<Pair<OperationDeclaration, MethodElement>> errorOperations = collectErrorOperations(declaration);

      if (errorAnnotation.isPresent()) {
        ErrorTypeDefinition<?>[] errorTypes = (ErrorTypeDefinition<?>[]) errorAnnotation.get().value().getEnumConstants();

        if (errorTypes.length > 0) {
          ErrorsModelFactory operationErrorModelDescriber = new ErrorsModelFactory(errorTypes, extensionNamespace);
          operationErrorModelDescriber.getErrorModels().forEach(declaration::addErrorModel);

          errorOperations.stream().forEach(pair -> registerOperationErrorTypes(pair.getSecond(), pair.getFirst(),
                                                                               operationErrorModelDescriber, errorTypes,
                                                                               extensionElement, typeWrapperCache));
        } else {
          handleNoErrorTypes(extensionElement, errorOperations);
        }
      } else {
        handleNoErrorTypes(extensionElement, errorOperations);
      }
    }
  }

  private void handleNoErrorTypes(ExtensionElement extensionElement,
                                  List<Pair<OperationDeclaration, MethodElement>> errorOperations)
      throws IllegalModelDefinitionException {

    long illegalOps = errorOperations.stream().filter(p -> p.getSecond().isAnnotatedWith(Throws.class)).count();
    if (illegalOps > 0) {
      throw new IllegalModelDefinitionException(format(
                                                       "There are %d operations annotated with @%s, but class %s does not specify any error type through the @%s annotation",
                                                       illegalOps, Throws.class.getSimpleName(),
                                                       extensionElement.getDeclaringClass().getName(),
                                                       ErrorTypes.class.getSimpleName()));
    }
  }

  private List<Pair<OperationDeclaration, MethodElement>> collectErrorOperations(ExtensionDeclaration declaration) {
    List<Pair<OperationDeclaration, MethodElement>> operations = new LinkedList<>();
    new IdempotentDeclarationWalker() {

      @Override
      public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
        Optional<ImplementingMethodModelProperty> modelProperty =
            declaration.getModelProperty(ImplementingMethodModelProperty.class);

        if (modelProperty.isPresent()) {
          operations.add(new Pair<>(declaration, new MethodWrapper(modelProperty.get().getMethod())));
        }
      }
    }.walk(declaration);

    return operations;
  }

  private void registerOperationErrorTypes(MethodElement operationMethod, OperationDeclaration operation,
                                           ErrorsModelFactory errorModelDescriber,
                                           ErrorTypeDefinition<?>[] extensionErrorTypes, ExtensionElement extensionElement,
                                           LoadingCache<Class<?>, TypeWrapper> typeWrapperCache) {
    getOperationThrowsDeclaration(operationMethod, extensionElement, typeWrapperCache)
        .ifPresent(throwsAnnotation -> {
          Class<? extends ErrorTypeProvider>[] providers = throwsAnnotation.value();
          Stream.of(providers).forEach(provider -> {
            try {
              ErrorTypeProvider errorTypeProvider = provider.newInstance();
              errorTypeProvider.getErrorTypes().stream()
                  .map(error -> validateOperationThrows(extensionErrorTypes, error))
                  .map(errorModelDescriber::getErrorModel)
                  .forEach(operation::addErrorModel);


            } catch (InstantiationException | IllegalAccessException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not create ErrorTypeProvider of type "
                  + provider.getName()), e);
            }
          });
        });
  }

  private Optional<Throws> getOperationThrowsDeclaration(MethodElement operationMethod, ExtensionElement extensionElement,
                                                         LoadingCache<Class<?>, TypeWrapper> typeWrapperCache) {
    TypeWrapper operationContainer = typeWrapperCache.getUnchecked(operationMethod.getDeclaringClass());
    return ofNullable(operationMethod.getAnnotation(Throws.class)
        .orElseGet(() -> operationContainer.getAnnotation(Throws.class)
            .orElseGet(() -> extensionElement.getAnnotation(Throws.class)
                .orElse(null))));
  }

  private ErrorTypeDefinition validateOperationThrows(ErrorTypeDefinition<?>[] errorTypes, ErrorTypeDefinition error) {
    Class<?> extensionErrorType = errorTypes.getClass().getComponentType();

    if (error.getClass().equals(MuleErrors.class)) {
      return error;
    }

    if (!error.getClass().equals(extensionErrorType) && !error.getClass().getSuperclass().equals(extensionErrorType)) {
      throw new IllegalModelDefinitionException(format("Invalid operation throws detected, the extension declared" +
          " to throw errors of %s type, but an error of %s type has been detected",
                                                       extensionErrorType, error.getClass()));
    } else {
      return error;
    }
  }
}
