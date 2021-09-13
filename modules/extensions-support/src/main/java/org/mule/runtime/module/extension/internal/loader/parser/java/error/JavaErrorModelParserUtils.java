/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.error;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getInfoFromAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getInfoFromExtension;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.enricher.ErrorsModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JavaErrorModelParserUtils {

  private static <E extends Enum<E>> ErrorModelParser toParser(ErrorTypeDefinition<E> errorTypeDefinition, String namespace) {
    return new JavaErrorModelParser(
        errorTypeDefinition.getType(),
        namespace,
        errorTypeDefinition.getParent().map(p -> toParser(p, namespace)).orElse(null));
}

  private static <E extends Enum<E>> ErrorModelParser toParser(org.mule.sdk.api.error.ErrorTypeDefinition<E> errorTypeDefinition, String namespace) {
    return new JavaErrorModelParser(
        errorTypeDefinition.getType(),
        namespace,
        errorTypeDefinition.getParent().map(p -> toParser(p, namespace)).orElse(null));
  }

  public static List<ErrorModelParser> getErrorModelParsers(ExtensionElement element, String namespace) {
    return getInfoFromExtension(element,
        ErrorTypes.class,
        org.mule.sdk.api.annotation.error.ErrorTypes.class,
        errorAnnotation ->
          Stream.of((ErrorTypeDefinition<?>[]) errorAnnotation.value().getEnumConstants())
              .map(def -> toParser(def, namespace))
              .collect(toList()),
        errorAnnotation -> Stream.of((org.mule.sdk.api.error.ErrorTypeDefinition<?>[]) errorAnnotation.value().getEnumConstants())
            .map(def -> toParser(def, namespace))
            .collect(toList())
    ).orElse(new LinkedList<>());
  }

  public static List<ErrorModelParser> getErrorModelParsers(OperationElement operation) {
    return getInfoFromAnnotation(operation,
        Throws.class,
        org.mule.sdk.api.annotation.error.Throws.class,
        ann -> {
          Class<? extends ErrorTypeProvider>[] providers = ann.value();
          for (Class<? extends ErrorTypeProvider> providerClass : providers) {
            try {
              ErrorTypeProvider errorTypeProvider = providerClass.newInstance();
              errorTypeProvider.getErrorTypes().stream()
                  .map(error -> validateOperationThrows(extensionErrorTypes, error))
                  .map(errorModelDescriber::getErrorModel)
                  .forEach(operation::addErrorModel);


            } catch (InstantiationException | IllegalAccessException e) {
              throw new MuleRuntimeException(createStaticMessage("Could not create ErrorTypeProvider of type "
                  + providerClass.getName()), e);
            }
          }
        },
        ann -> {
        },
        () -> {
        }
    );
  }
    private static ErrorTypeDefinition validateOperationThrows(ErrorTypeDefinition<?>[] errorTypes, ErrorTypeDefinition error) {
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

  private JavaErrorModelParserUtils() {
  }
}
