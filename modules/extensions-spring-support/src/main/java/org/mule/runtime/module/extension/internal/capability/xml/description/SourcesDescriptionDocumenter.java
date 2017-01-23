/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static java.util.Collections.emptyList;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.extension.api.annotation.Sources;

import java.util.List;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * {@link AbstractDescriptionDocumenter} implementation that fills {@link WithSourcesDeclaration}s
 *
 * @since 4.0
 */
final class SourcesDescriptionDocumenter extends AbstractDescriptionDocumenter<WithSourcesDeclaration<?>> {

  private final ParameterDescriptionDocumenter parameterDeclarer;

  SourcesDescriptionDocumenter(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    this.parameterDeclarer = new ParameterDescriptionDocumenter(processingEnv);
  }

  void document(WithSourcesDeclaration<?> declaration, TypeElement element) {
    getSourceClasses(processingEnv, element)
        .forEach(sourceElement -> findMatchingSource(declaration, sourceElement)
            .ifPresent(source -> {
              source.setDescription(processor.getJavaDocSummary(processingEnv, sourceElement));
              parameterDeclarer.document(source, sourceElement);
            }));
  }

  private Optional<SourceDeclaration> findMatchingSource(WithSourcesDeclaration<?> declaration, Element element) {
    return declaration.getMessageSources().stream()
        .filter(provider -> {
          String name = provider.getName();
          String alias = getAliasValue(element);
          String defaultNaming = hyphenize(element.getSimpleName().toString());
          return name.equals(defaultNaming) || name.equals(alias);
        })
        .findAny();
  }

  private List<TypeElement> getSourceClasses(ProcessingEnvironment processingEnv, Element element) {
    Sources sourcesAnnotation = processor.getAnnotationFromType(processingEnv, (TypeElement) element, Sources.class);
    if (sourcesAnnotation == null) {
      return emptyList();
    }
    return processor.getAnnotationClassesValue(element, Sources.class, sourcesAnnotation.value());
  }
}
