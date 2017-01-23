/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.module.extension.internal.capability.xml.schema.ExtensionAnnotationProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Generic contract for classes that adds descriptions to a declaration using a {@link ProcessingEnvironment} to
 * enrich the descriptions with the javadocs extracted from the extension's acting classes.
 * <p>
 * This is necessary because such documentation is not available on runtime.
 *
 * @param <T> the type to document.
 * @since 4.0
 */
abstract class AbstractDescriptionDocumenter<T> {

  protected static final ExtensionAnnotationProcessor processor = new ExtensionAnnotationProcessor();

  final ProcessingEnvironment processingEnv;

  AbstractDescriptionDocumenter(ProcessingEnvironment processingEnvironment) {
    this.processingEnv = processingEnvironment;
  }

  /**
   * Sets the descriptions for the provided {@code declaration} using the javadocs in the source code.
   *
   * @param declaration   the declaration to document
   * @param configElement the type element associated to the declaration.
   */
  abstract void document(T declaration, TypeElement configElement);


  String getAliasValue(Element element) {
    Alias alias = element.getAnnotation(Alias.class);
    return alias != null ? alias.value() : "";
  }

}
