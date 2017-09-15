/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.util.ComponentLocationProvider;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

/**
 * Provides a standard way to generate a log entry or message that references an element in a flow.
 *
 * @since 3.8.0
 */
public abstract class LocationExecutionContextProvider extends ComponentLocationProvider implements ExceptionContextProvider {

  protected static final QName SOURCE_ELEMENT_ANNOTATION_KEY =
      new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceElement");

  private static final Pattern URL_PATTERN = compile("url=\"[a-z]*://([^@]*)@");
  private static final Pattern ADDRESS_PATTERN = compile("address=\"[a-z]*://([^@]*)@");
  private static final Pattern PASSWORD_PATTERN = compile("password=\"([^\"]*)\"");
  private static final String PASSWORD_MASK = "<<credentials>>";
  public static final String PASSWORD_ATTRIBUTE_MASK = "password=\"%s\"";

  /**
   * Populates the passed beanAnnotations with the other passed parameters.
   *
   * @param beanAnnotations the map with annotations to populate
   * @param xmlContent the xml representation of the element definition.
   */
  public static void addMetadataAnnotationsFromXml(Map<QName, Object> beanAnnotations, String xmlContent) {
    beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, xmlContent);
  }

  protected static String getSourceXML(Component element) {
    Object sourceXml = element.getAnnotation(SOURCE_ELEMENT_ANNOTATION_KEY);
    return sourceXml != null ? maskPasswords(sourceXml.toString()) : null;
  }

  protected static String maskPasswords(String xml) {
    xml = maskUrlPassword(xml, URL_PATTERN);
    xml = maskUrlPassword(xml, ADDRESS_PATTERN);

    Matcher matcher = PASSWORD_PATTERN.matcher(xml);
    if (matcher.find() && matcher.groupCount() > 0) {
      xml = xml.replaceAll(maskPasswordAttribute(matcher.group(1)), maskPasswordAttribute(PASSWORD_MASK));
    }
    xml = maskUrlPassword(xml, PASSWORD_PATTERN);

    return xml;
  }

  private static String maskUrlPassword(String xml, Pattern pattern) {
    Matcher matcher = pattern.matcher(xml);
    if (matcher.find() && matcher.groupCount() > 0) {
      xml = xml.replaceAll(matcher.group(1), PASSWORD_MASK);
    }
    return xml;
  }

  private static String maskPasswordAttribute(String password) {
    return format(PASSWORD_ATTRIBUTE_MASK, password);
  }

}
