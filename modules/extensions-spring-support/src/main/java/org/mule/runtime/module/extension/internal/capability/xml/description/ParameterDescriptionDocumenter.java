/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.internal.capability.xml.schema.MethodDocumentation;

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * {@link AbstractDescriptionDocumenter} implementation that fills {@link ParameterizedDeclaration}s
 *
 * @since 4.0
 */
final class ParameterDescriptionDocumenter extends AbstractDescriptionDocumenter<ParameterizedDeclaration<?>> {

  ParameterDescriptionDocumenter(ProcessingEnvironment processingEnv) {
    super(processingEnv);
  }

  /**
   * Describes parameters that are defined as Method parameters.
   */
  void document(ParameterizedDeclaration<?> parameterized, MethodDocumentation documentation) {
    parameterized.getAllParameters().forEach(p -> {
      String description = documentation.getParameters().get(p.getName());
      if (description != null) {
        p.setDescription(description);
      }
    });
  }

  @Override
  void document(ParameterizedDeclaration<?> parameterized, final TypeElement element) {
    final Map<String, VariableElement> variableElements = processor.getFieldsAnnotatedWith(element, Parameter.class);
    TypeElement traversingElement = element;
    while (traversingElement != null && !Object.class.getName().equals(traversingElement.getQualifiedName().toString())) {
      Class<?> declaringClass = processor.classFor(traversingElement, processingEnv).get();

      parameterized.getAllParameters()
          .forEach(param -> getField(declaringClass, param)
              .filter(field -> variableElements.containsKey(field.getName()))
              .ifPresent(field -> {
                String summary = processor.getJavaDocSummary(processingEnv, variableElements.get(field.getName()));
                param.setDescription(summary);
              }));
      traversingElement = (TypeElement) processingEnv.getTypeUtils().asElement(traversingElement.getSuperclass());
    }

    for (VariableElement variableElement : processor.getFieldsAnnotatedWith(element, ParameterGroup.class)
        .values()) {
      TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(variableElement.asType());
      document(parameterized, typeElement);
    }
  }
}
