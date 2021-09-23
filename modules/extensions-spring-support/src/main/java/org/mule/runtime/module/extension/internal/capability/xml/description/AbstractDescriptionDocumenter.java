/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import org.mule.runtime.extension.internal.loader.util.JavaParserUtils;
import org.mule.runtime.module.extension.internal.capability.xml.schema.ExtensionAnnotationProcessor;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Generic contract for classes that adds descriptions to a declaration using a {@link ProcessingEnvironment} to enrich the
 * descriptions with the javadocs extracted from the extension's acting classes.
 * <p>
 * This is necessary because such documentation is not available on runtime.
 *
 * @param <T> the type to document.
 * @since 4.0
 */
abstract class AbstractDescriptionDocumenter {

  protected static final ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();
  protected final TypeElement objectType;
  protected static final String VALUE_PROPERTY = "value";


  final ProcessingEnvironment processingEnv;


  AbstractDescriptionDocumenter(ProcessingEnvironment processingEnvironment) {
    this.processingEnv = processingEnvironment;
    objectType = processingEnv.getElementUtils().getTypeElement(Object.class.getCanonicalName());
  }

  String getNameOrAlias(Element element) {
    return getAlias(element).orElse(element.getSimpleName().toString());
  }

  Optional<String> getAlias(Element element) {
    return ofNullable(JavaParserUtils.getAlias(element::getAnnotation, () -> null));
  }

  Map<String, Element> getApiMethods(ProcessingEnvironment processingEnv, List<TypeElement> containerClasses) {
    Map<String, Element> methods = new HashMap<>();

    for (TypeElement containerElement : containerClasses) {

      TypeElement currentElement = containerElement;
      while (currentElement != null
          && !processingEnv.getTypeUtils().isSameType(currentElement.asType(), objectType.asType())) {
        for (ExecutableElement operation : IntrospectionUtils.getApiMethods(currentElement, processingEnv)) {
          currentElement.getEnclosedElements().stream()
              .filter(e -> e.getSimpleName().toString().equals(operation.getSimpleName().toString()))
              .findFirst()
              .ifPresent(e -> {
                String nameOrAlias = getNameOrAlias(operation);
                if (!methods.containsKey(nameOrAlias) || e.getEnclosingElement().equals(containerElement)) {
                  // Use the leaf in an overriden method hierarchy
                  methods.put(nameOrAlias, e);
                }
              });
        }
        Element superClass = processingEnv.getTypeUtils().asElement(currentElement.getSuperclass());
        if (superClass instanceof TypeElement) {
          currentElement = (TypeElement) superClass;
        } else {
          currentElement = null;
        }
      }
    }
    return unmodifiableMap(methods);
  }

}
