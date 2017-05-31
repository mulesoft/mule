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
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
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

import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used {@link ErrorTypes} and {@link Throws} in an Annotation based extension.
 *
 * @since 4.0
 */
public class ErrorsDeclarationEnricher implements DeclarationEnricher {

  private ErrorsModelFactory errorModelDescriber;
  private ExtensionElement extensionElement;
  private final LoadingCache<Class<?>, TypeWrapper> typeWrapperCache =
      CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, TypeWrapper>() {

        @Override
        public TypeWrapper load(Class<?> clazz) throws Exception {
          return new TypeWrapper(clazz);
        }
      });

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    Optional<ImplementingTypeModelProperty> implementingType = declaration.getModelProperty(ImplementingTypeModelProperty.class);
    String extensionNamespace = getExtensionsErrorNamespace(declaration);
    errorModelDescriber = new ErrorsModelFactory(extensionNamespace);
    errorModelDescriber.getErrorModels().forEach(declaration::addErrorModel);

    if (implementingType.isPresent()) {

      extensionElement = new ExtensionTypeWrapper<>(implementingType.get().getType());
      extensionElement.getAnnotation(ErrorTypes.class).ifPresent(errorTypesAnnotation -> {

        ErrorTypeDefinition<?>[] errorTypes = (ErrorTypeDefinition<?>[]) errorTypesAnnotation.value().getEnumConstants();

        if (errorTypes.length > 0) {
          errorModelDescriber = new ErrorsModelFactory(errorTypes, extensionNamespace);
          errorModelDescriber.getErrorModels().forEach(declaration::addErrorModel);

          new IdempotentDeclarationWalker() {

            @Override
            public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
              Optional<ImplementingMethodModelProperty> modelProperty =
                  declaration.getModelProperty(ImplementingMethodModelProperty.class);

              modelProperty.ifPresent(implementingMethodModelProperty -> {
                MethodElement methodElement = new MethodWrapper(implementingMethodModelProperty.getMethod());
                registerOperationErrorTypes(methodElement, declaration, errorModelDescriber, errorTypes);
              });
            }
          }.walk(declaration);
        }
      });
    }
  }

  private void registerOperationErrorTypes(MethodElement operationMethod, OperationDeclaration operation,
                                           ErrorsModelFactory errorModelDescriber,
                                           ErrorTypeDefinition<?>[] extensionErrorTypes) {
    getOperationThrowsDeclaration(operationMethod, extensionElement)
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

  private Optional<Throws> getOperationThrowsDeclaration(MethodElement operationMethod, ExtensionElement extensionElement) {
    TypeWrapper operationContainer = typeWrapperCache.getUnchecked(operationMethod.getDeclaringClass());
    return ofNullable(operationMethod.getAnnotation(Throws.class)
        .orElseGet(() -> operationContainer.getAnnotation(Throws.class)
            .orElseGet(() -> extensionElement.getAnnotation(Throws.class)
                .orElse(null))));
  }

  private ErrorTypeDefinition validateOperationThrows(ErrorTypeDefinition<?>[] errorTypes, ErrorTypeDefinition error) {
    Class<?> extensionErrorType = errorTypes.getClass().getComponentType();

    if (!error.getClass().equals(extensionErrorType) && !error.getClass().getSuperclass().equals(extensionErrorType)) {
      throw new IllegalModelDefinitionException(format("Invalid operation throws detected, the extension declared" +
          " to throw errors of %s type, but an error of %s type has been detected", extensionErrorType, error.getClass()));
    } else {
      return error;
    }
  }
}
