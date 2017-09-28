/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.parsers;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Stores the metadata annotations from the XML parser so they are available when building the actual objects of the application.
 */
public class DefaultXmlMetadataAnnotations implements XmlMetadataAnnotations {

  public static final String METADATA_ANNOTATIONS_KEY = "metadataAnnotations";

  private StringBuilder xmlContent = new StringBuilder();
  private int lineNumber;

  /**
   * Builds the opening tag of the xml element.
   * 
   * @param qName the qualified name of the element
   * @param atts the attributes of the element, with the qualified name as key
   */
  @Override
  public void appendElementStart(String qName, Map<String, String> atts) {
    xmlContent.append("<" + qName);
    for (Entry<String, String> entry : atts.entrySet()) {
      xmlContent.append(" " + entry.getKey() + "=\"" + entry.getValue() + "\"");
    }
    xmlContent.append(">");
  }

  /**
   * Adds the body of the xml tag.
   * 
   * @param elementBody the body content to be added
   */
  @Override
  public void appendElementBody(String elementBody) {
    xmlContent.append(elementBody);
  }

  /**
   * Builds the closing tag of the xml element.
   * 
   * @param qName the qualified name of the element
   */
  @Override
  public void appendElementEnd(String qName) {
    xmlContent.append("</" + qName + ">");
  }

  /**
   * @return the reconstruction of the declaration of the element in its source xml file.
   *         <p/>
   *         Note that the order of the elements may be different, and any implicit attributes with default values will be
   *         included.
   */
  @Override
  public String getElementString() {
    return xmlContent.toString()
        .replaceAll(">\\s+<+", ">" + LINE_SEPARATOR + "<") /* compact whitespaces and line breaks */
        .trim();
  }

  /**
   * @param lineNumber the line where the declaration of the element starts in its source xml file.
   */
  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  /**
   * @return the line where the declaration of the element starts in its source xml file.
   */
  @Override
  public int getLineNumber() {
    return lineNumber;
  }
}
