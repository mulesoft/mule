/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static java.util.Collections.singletonList;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.module.extension.internal.capability.xml.schema.MethodDocumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * {@link AbstractDescriptionDocumenter} implementation that fills {@link WithSourcesDeclaration}s
 *
 * @since 4.0
 */
final class SourcesDescriptionDocumenter extends AbstractDescriptionDocumenter {

  private final ParameterDescriptionDocumenter parameterDeclarer;

  SourcesDescriptionDocumenter(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    this.parameterDeclarer = new ParameterDescriptionDocumenter(processingEnv);
  }

  void document(TypeElement element, WithSourcesDeclaration<?>... containerDeclarations) {
    getSourceClasses(processingEnv, element)
        .forEach(sourceElement -> findMatchingSource(containerDeclarations, sourceElement)
            .ifPresent(source -> {
              source.setDescription(processor.getJavaDocSummary(processingEnv, sourceElement));
              parameterDeclarer.document(source, sourceElement);

              Map<String, Element> methods = getApiMethods(processingEnv, singletonList(sourceElement));
              source.getSuccessCallback().ifPresent(cb -> documentCallback(methods, cb));
              source.getErrorCallback().ifPresent(cb -> documentCallback(methods, cb));
            }));
  }

  private void documentCallback(Map<String, Element> methods, SourceCallbackDeclaration cb) {
    Element method = methods.get(cb.getName());
    if (method != null) {
      MethodDocumentation documentation = processor.getMethodDocumentation(processingEnv, method);
      parameterDeclarer.document(cb, method, documentation);
    }
  }

  private Optional<SourceDeclaration> findMatchingSource(WithSourcesDeclaration<?>[] containerDeclarations, Element element) {
    for (WithSourcesDeclaration<?> declaration : containerDeclarations) {
      Optional<SourceDeclaration> sourceDeclaration = declaration.getMessageSources().stream()
          .filter(source -> {
            String name = source.getName();
            String elementName = element.getSimpleName().toString();
            String defaultNaming = hyphenize(elementName);
            return name.equals(defaultNaming) || getAlias(element).map(name::equals).orElse(name.equals(elementName));
          })
          .findFirst();

      if (sourceDeclaration.isPresent()) {
        return sourceDeclaration;
      }
    }

    return Optional.empty();
  }

  private List<TypeElement> getSourceClasses(ProcessingEnvironment processingEnv, Element element) {
    List<TypeElement> elements = new ArrayList<>();
    elements.addAll(processor.getArrayClassAnnotationValue(element, Sources.class, VALUE_PROPERTY, processingEnv));
    elements.addAll(processor.getArrayClassAnnotationValue(element, org.mule.sdk.api.annotation.Sources.class, VALUE_PROPERTY,
                                                           processingEnv));

    return elements;
  }
}
