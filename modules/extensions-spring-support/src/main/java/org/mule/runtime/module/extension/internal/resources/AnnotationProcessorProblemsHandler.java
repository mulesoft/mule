/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.loader.ProblemsHandler;
import org.mule.runtime.module.extension.api.loader.java.type.WithElement;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import java.util.Optional;
import java.util.function.Function;

/**
 * {@link ProblemsHandler} implementation to use when loading an extension using the annotation processor.
 *
 * @since 4.1
 */
public final class AnnotationProcessorProblemsHandler implements ProblemsHandler {

  private final Messager messager;

  AnnotationProcessorProblemsHandler(ProcessingEnvironment processingEnvironment) {
    messager = processingEnvironment.getMessager();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleProblems(ProblemsReporter problemsReporter) {
    problemsReporter.getWarnings().forEach(problem -> messager
        .printMessage(WARNING, problem.getMessage(), getElement(problem.getComponent())));
    problemsReporter.getErrors().forEach(problem -> messager
        .printMessage(ERROR, problem.getMessage(), getElement(problem.getComponent())));
  }

  private Element getElement(NamedObject component) {
    if (component instanceof EnrichableModel) {
      EnrichableModel enrichableModel = (EnrichableModel) component;

      Element element;
      element = getElement(enrichableModel, ExtensionOperationDescriptorModelProperty.class,
                           mp -> mp.getOperationElement().getElement());
      if (element != null) {
        return element;
      }

      element = getElement(enrichableModel, ExtensionParameterDescriptorModelProperty.class,
                           mp -> mp.getExtensionParameter().getElement());
      if (element != null) {
        return element;
      }

      element = getElement(enrichableModel, ExtensionTypeDescriptorModelProperty.class,
                           mp -> mp.getType().getElement());
      if (element != null) {
        return element;
      }
    }
    if (component instanceof WithElement) {
      Optional<? extends Element> optionalElement = ((WithElement) component).getElement();
      if (optionalElement.isPresent()) {
        return optionalElement.get();
      }
    }
    return null;
  }

  private <T extends ModelProperty> Element getElement(EnrichableModel enrichableModel, Class<T> modelPropertyClass,
                                                       Function<T, Optional<? extends Element>> elementFunction) {
    return enrichableModel.getModelProperty(modelPropertyClass)
        .flatMap(elementFunction::apply)
        .orElse(null);
  }
}
