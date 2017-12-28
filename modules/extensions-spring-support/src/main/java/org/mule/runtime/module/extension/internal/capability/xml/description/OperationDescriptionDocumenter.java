/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.module.extension.internal.capability.xml.schema.MethodDocumentation;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.ConfigurationASTElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.OperationElementAST;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link AbstractDescriptionDocumenter} implementation that fills {@link WithOperationsDeclaration}s
 *
 * @since 4.0
 */
final class OperationDescriptionDocumenter extends AbstractDescriptionDocumenter<WithOperationsDeclaration<?>> {

  private final ParameterDescriptionDocumenter parameterDeclarer;

  OperationDescriptionDocumenter(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    this.parameterDeclarer = new ParameterDescriptionDocumenter(processingEnv);
  }

  void document(WithOperationsDeclaration<?> declaration, TypeElement element) {
    final Map<String, Element> methods = getAllOperations(processingEnv, element);
    try {
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
        parameterDeclarer.document(operation, documentation);
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception found while trying to obtain descriptions"), e);
    }
  }

  private Map<String, Element> getAllOperations(ProcessingEnvironment processingEnv, Element element) {
    Map<String, Element> elements = new LinkedHashMap<>();
    Configurations configurations = processor.getAnnotationFromType(processingEnv, (TypeElement) element, Configurations.class);
    if (configurations != null) {
      List<TypeElement> configs = processor.getAnnotationClassesValue(element, Configurations.class, configurations.value());
      configs.forEach(c -> elements.putAll(getOperationMethodElements(processingEnv, c)));
    }
    elements.putAll(getOperationMethodElements(processingEnv, element));
    return elements;
  }

  private Map<String, Element> getOperationMethodElements(ProcessingEnvironment processingEnv, Element withOperationsElement) {
    return new ConfigurationASTElement((TypeElement) withOperationsElement, processingEnv).getOperationContainers().stream()
        .map(OperationContainerElement::getOperations)
        .flatMap(Collection::stream)
        .collect(toMap(NamedObject::getName, op -> ((OperationElementAST) op).getExecutableElement()));
  }
}
