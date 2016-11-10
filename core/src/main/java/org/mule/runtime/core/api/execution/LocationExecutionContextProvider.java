/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.util.ComponentLocationProvider;

import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Provides a standard way to generate a log entry or message that references an element in a flow.
 * 
 * @since 3.8.0
 */
public abstract class LocationExecutionContextProvider extends ComponentLocationProvider implements ExceptionContextProvider {

  private static final QName SOURCE_ELEMENT_ANNOTATION_KEY =
      new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceElement");

  /**
   * Populates the passed beanAnnotations with the other passed parameters.
   * 
   * @param beanAnnotations the map with annotations to populate
   * @param fileName the name of the file where the element definition was read from.
   * @param lineNumber the line number where the definition of the element starts in the file.
   * @param xmlContent the xml representation of the element definition.
   */
  public static void addMetadataAnnotationsFromXml(Map<QName, Object> beanAnnotations, String fileName, int lineNumber,
                                                   String xmlContent) {
    beanAnnotations.put(SOURCE_FILE_ANNOTATION_KEY, fileName);
    beanAnnotations.put(SOURCE_FILE_LINE_ANNOTATION_KEY, lineNumber);
    beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, xmlContent);
  }

  protected static String getSourceXML(AnnotatedObject element) {
    Object sourceXml = element.getAnnotation(SOURCE_ELEMENT_ANNOTATION_KEY);
    return sourceXml != null ? sourceXml.toString() : null;
  }

}
