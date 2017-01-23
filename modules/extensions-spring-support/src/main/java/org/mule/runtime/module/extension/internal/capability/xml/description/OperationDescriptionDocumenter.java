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
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.module.extension.internal.capability.xml.schema.MethodDocumentation;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

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
    final Map<String, Element> methods = getOperationMethods(processingEnv, element);
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

  /**
   * Scans all the classes annotated with {@link Extension}, takes the {@link Operations} from those and returns the methods which
   * are public and are not annotated with {@link Ignore}
   *
   * @return a {@link Map} which keys are the method names and the values are the method represented as a {@link Element}
   */
  private Map<String, Element> getOperationMethods(ProcessingEnvironment processingEnv, Element element) {
    ImmutableMap.Builder<String, Element> methods = ImmutableMap.builder();
    Operations operationsAnnotation = processor.getAnnotationFromType(processingEnv, (TypeElement) element, Operations.class);
    if (operationsAnnotation != null) {
      final Class<?>[] operationsClasses = operationsAnnotation.value();
      List<AnnotationValue> annotationValues = processor.getAnnotationValue(element, Operations.class);
      for (Class<?> operationClass : operationsClasses) {
        Element operationClassElement = processor.getElementForClass(annotationValues, operationClass);
        if (operationClassElement != null) {
          for (Method operation : IntrospectionUtils.getOperationMethods(operationClass)) {
            operationClassElement.getEnclosedElements().stream()
                .filter(e -> e.getSimpleName().toString().equals(operation.getName())).findFirst()
                .ifPresent(operationMethodElement -> methods.put(operation.getName(), operationMethodElement));
          }
        }
      }
    }
    return methods.build();
  }
}
