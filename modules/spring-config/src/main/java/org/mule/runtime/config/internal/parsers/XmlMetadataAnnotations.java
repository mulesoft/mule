/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.parsers;

import java.util.Map;

/**
 * Stores the metadata annotations from the XML parser so they are available when building the actual objects of the application.
 */
public interface XmlMetadataAnnotations {

  String METADATA_ANNOTATIONS_KEY = "metadataAnnotations";

  /**
   * Builds the opening tag of the xml element.
   * 
   * @param qName the qualified name of the element
   * @param atts the attributes of the element, with the qualified name as key
   */
  void appendElementStart(String qName, Map<String, String> atts);

  /**
   * Adds the body of the xml tag.
   * 
   * @param elementBody the body content to be added
   */
  void appendElementBody(String elementBody);

  /**
   * Builds the closing tag of the xml element.
   * 
   * @param qName the qualified name of the element
   */
  void appendElementEnd(String qName);

  /**
   * @return the reconstruction of the declaration of the element in its source xml file.
   *         <p/>
   *         Note that the order of the elements may be different, and any implicit attributes with default values will be
   *         included.
   */
  String getElementString();

  /**
   * @return the line where the declaration of the element starts in its source xml file.
   */
  int getLineNumber();
}
