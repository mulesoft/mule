/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.error;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.ENUM_CONSTANT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.error.ErrorModelUtils.isMuleError;
import static org.mule.runtime.module.extension.internal.loader.delegate.MuleExtensionAnnotationParser.mapReduceAnnotation;
import static org.mule.runtime.module.extension.internal.loader.delegate.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.error.AstElementErrorTypeDefinitionAdapter;
import org.mule.runtime.module.extension.internal.error.SdkErrorTypeDefinitionAdapter;
import org.mule.runtime.module.extension.internal.error.SdkErrorTypeProviderAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.runtime.exception.DefaultExceptionHandlerFactory;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Utilities for parsing Java defined {@link ErrorModel error models}
 *
 * @since 4.5.0
 */
public final class JavaErrorModelParserUtils {

  /**
   * Parses the error types defined at the extension level
   *
   * @param element the {@link ExtensionElement}
   * @return a list of {@link ErrorModelParser}.
   */
  public static List<ErrorModelParser> parseExtensionErrorModels(ExtensionElement element) {
    return mapReduceSingleAnnotation(element,
                                     ErrorTypes.class,
                                     org.mule.sdk.api.annotation.error.ErrorTypes.class,
                                     value -> parseErrorTypeDefinitions(value.getClassValue(ErrorTypes::value)),
                                     value -> parseErrorTypeDefinitions(value
                                         .getClassValue(org.mule.sdk.api.annotation.error.ErrorTypes::value)))
                                             .orElse(new LinkedList<>());
  }

  /**
   * Parses the errors defined by the {@code operation}
   *
   * @param extensionParser  the extension's parser
   * @param extensionElement the extension's element
   * @param operation        the operation element
   * @return a list of {@link ErrorModelParser}
   */
  public static List<ErrorModelParser> parseOperationErrorModels(ExtensionModelParser extensionParser,
                                                                 ExtensionElement extensionElement,
                                                                 OperationElement operation) {
    return getThrowsDeclaration(operation, extensionElement)
        .flatMap(withThrows -> mapReduceAnnotation(
                                                   withThrows,
                                                   Throws.class,
                                                   org.mule.sdk.api.annotation.error.Throws.class,
                                                   ann -> parseErrorTypeProviders(ann.getClassArrayValue(Throws::value),
                                                                                  extensionParser),
                                                   ann -> parseErrorTypeProviders(ann
                                                       .getClassArrayValue(org.mule.sdk.api.annotation.error.Throws::value),
                                                                                  extensionParser),
                                                   dualThrowsException(operation)))
        .orElse(new LinkedList<>());
  }

  /**
   * Returns the root {@link Class} for the given {@code errorTypeDefinition}.
   * <p>
   * Use this method to avoid dealing with legacy adapters
   *
   * @param errorTypeDefinition the error type definition
   * @return the root definition's {@link Class}
   */
  public static Class<?> getDeclarationClass(ErrorTypeDefinition errorTypeDefinition) {
    return errorTypeDefinition instanceof SdkErrorTypeDefinitionAdapter
        ? ((SdkErrorTypeDefinitionAdapter<?>) errorTypeDefinition).getDelegate().getClass()
        : errorTypeDefinition.getClass();
  }

  public static java.util.Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty(WithAnnotations element,
                                                                                                   String elementType,
                                                                                                   String elementName) {
    Optional<Type> classValue = mapReduceSingleAnnotation(
                                                          element,
                                                          elementType,
                                                          elementName,
                                                          OnException.class,
                                                          org.mule.sdk.api.annotation.OnException.class,
                                                          ann -> ann.getClassValue(OnException::value),
                                                          ann -> ann
                                                              .getClassValue(org.mule.sdk.api.annotation.OnException::value));

    return classValue
        .flatMap(c -> c.getDeclaringClass())
        .map(clazz -> new ExceptionHandlerModelProperty(new DefaultExceptionHandlerFactory(clazz)));
  }

  private static List<ErrorModelParser> parseErrorTypeDefinitions(Type type) {
    Map<ErrorTypeDefinition, ErrorModelParser> cycleControl = new HashMap<>(emptyMap());

    if (type.getDeclaringClass().isPresent()) {
      Class<Enum> enumClass = (Class<Enum>) type.getDeclaringClass().get();
      return Stream.of(enumClass.getEnumConstants())
          .map(def -> toParser(SdkErrorTypeDefinitionAdapter.from(def), cycleControl))
          .collect(toList());
    } else {
      return type.getElement()
          .map(element -> element.getEnclosedElements().stream()
              .filter(enclosed -> enclosed.getKind().equals(ENUM_CONSTANT))
              .map(def -> toParser(new AstElementErrorTypeDefinitionAdapter(def), cycleControl))
              .collect(toList()))
          .orElse(emptyList());
    }
  }

  private static void validateOperationThrows(ExtensionModelParser extensionParser, ErrorTypeDefinition error) {
    Class<?> errorDefinitionClass = JavaErrorModelParserUtils.getDeclarationClass(error);

    if (isMuleError(error) || extensionParser.getErrorModelParsers().isEmpty()) {
      return;
    }

    JavaErrorModelParser errorModelParser = (JavaErrorModelParser) extensionParser.getErrorModelParsers().get(0);
    Class<?> extensionErrorType = errorModelParser.getErrorTypeDefinitionDeclarationClass();

    if (!errorDefinitionClass.equals(extensionErrorType) && !errorDefinitionClass.getSuperclass().equals(extensionErrorType)) {
      throw new IllegalModelDefinitionException(format("Invalid operation throws detected, the extension declared" +
          " to throw errors of %s type, but an error of %s type has been detected",
                                                       extensionErrorType, error.getClass()));
    }
  }

  private static ErrorModelParser toParser(ErrorTypeDefinition<?> errorTypeDefinition,
                                           Map<ErrorTypeDefinition, ErrorModelParser> cycleControl) {
    JavaErrorModelParser parser = new JavaErrorModelParser(errorTypeDefinition, isMuleError(errorTypeDefinition));
    cycleControl.put(errorTypeDefinition, parser);
    parser.setParent(errorTypeDefinition.getParent().map(p -> {
      ErrorModelParser parentParser = cycleControl.get(p);
      if (parentParser == null) {
        parentParser = toParser(p, cycleControl);
        cycleControl.put(p, parentParser);
      }
      return parentParser;
    }));

    return parser;
  }

  private static List<ErrorModelParser> parseErrorTypeProviders(List<Type> providerTypes,
                                                                ExtensionModelParser extensionParser) {
    Map<ErrorTypeDefinition, ErrorModelParser> cycleControl = new HashMap<>();
    return providerTypes.stream()
        .filter(type -> type.getDeclaringClass().isPresent())
        .flatMap(type -> {
          try {
            org.mule.sdk.api.annotation.error.ErrorTypeProvider errorTypeProvider =
                SdkErrorTypeProviderAdapter.from(type.getDeclaringClass().get().newInstance());
            return errorTypeProvider.getErrorTypes().stream()
                .map(error -> {
                  validateOperationThrows(extensionParser, error);
                  return toParser(error, cycleControl);
                });
          } catch (InstantiationException | IllegalAccessException e) {
            throw new MuleRuntimeException(createStaticMessage("Could not create ErrorTypeProvider of type "
                + type.getName()), e);
          }
        })
        .collect(toList());
  }

  private static Optional<WithAnnotations> getThrowsDeclaration(MethodElement operationMethod, Type extensionElement) {
    if (hasThrowsDeclaration(operationMethod)) {
      return of(operationMethod);
    } else {
      Type operationContainer = operationMethod.getEnclosingType();
      if (hasThrowsDeclaration(operationContainer)) {
        return of(operationContainer);
      } else if (hasThrowsDeclaration(extensionElement)) {
        return of(extensionElement);
      }
    }

    return empty();
  }

  private static boolean hasThrowsDeclaration(WithAnnotations annotated) {
    return annotated.isAnnotatedWith(Throws.class) || annotated.isAnnotatedWith(org.mule.sdk.api.annotation.error.Throws.class);
  }

  private static Supplier<IllegalModelDefinitionException> dualThrowsException(OperationElement operation) {
    return () -> new IllegalOperationModelDefinitionException(
                                                              format("Operation '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                     operation.getAlias(),
                                                                     Throws.class.getName(),
                                                                     org.mule.sdk.api.annotation.error.Throws.class.getName()));
  }

  private JavaErrorModelParserUtils() {}
}
