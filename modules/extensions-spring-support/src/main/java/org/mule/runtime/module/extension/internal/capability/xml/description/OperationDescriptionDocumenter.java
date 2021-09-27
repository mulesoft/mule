/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.module.extension.internal.capability.xml.schema.MethodDocumentation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * {@link AbstractDescriptionDocumenter} implementation that fills {@link WithOperationsDeclaration}s
 *
 * @since 4.0
 */
final class OperationDescriptionDocumenter extends AbstractDescriptionDocumenter {

  private final ParameterDescriptionDocumenter parameterDeclarer;

  OperationDescriptionDocumenter(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    this.parameterDeclarer = new ParameterDescriptionDocumenter(processingEnv);
  }

  void document(TypeElement element, WithOperationsDeclaration<?>... containerDeclarations) {
    final Map<String, Element> methods = getAllOperations(processingEnv, element);
    try {
      for (WithOperationsDeclaration<?> declaration : containerDeclarations) {
        documentOperations(declaration, methods);
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception found while trying to obtain descriptions"), e);
    }
  }

  private void documentOperations(WithOperationsDeclaration<?> declaration, Map<String, Element> methods) {
    for (OperationDeclaration operation : declaration.getOperations()) {
      Element method = methods.get(operation.getName());

      // there are two cases in which method can be null:
      // * A synthetic operation which was not defined in any class but added by a model property
      // * An extension which operations are defined across multiple classes and the one being processed is not
      // the one which defined the operation being processed
      if (method == null) {
        continue;
      }

      MethodDocumentation documentation = processor.getMethodDocumentation(processingEnv, method);
      operation.setDescription(documentation.getSummary());
      parameterDeclarer.document(operation, method, documentation);
    }
  }

  private Map<String, Element> getAllOperations(ProcessingEnvironment processingEnv, Element element) {
    Map<String, Element> elements = new LinkedHashMap<>();
    Consumer<TypeElement> consumer = c -> elements.putAll(getApiMethods(processingEnv, getOperationClasses(processingEnv, c)));

    processor.getArrayClassAnnotationValue(element, Configurations.class, VALUE_PROPERTY, processingEnv)
        .forEach(consumer);
    processor
        .getArrayClassAnnotationValue(element, org.mule.sdk.api.annotation.Configurations.class, VALUE_PROPERTY, processingEnv)
        .forEach(consumer);

    elements.putAll(getApiMethods(processingEnv, getOperationClasses(processingEnv, element)));
    return elements;
  }

  private List<TypeElement> getOperationClasses(ProcessingEnvironment processingEnv, Element element) {
    List<TypeElement> types = processor.getArrayClassAnnotationValue(element, Operations.class, VALUE_PROPERTY, processingEnv);
    types.addAll(processor.getArrayClassAnnotationValue(element, org.mule.sdk.api.annotation.Operations.class, VALUE_PROPERTY,
                                                        processingEnv));

    return types;
  }
}
