/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import org.mule.api.annotation.NoExtend;
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
@NoExtend
public abstract class LocationExecutionContextProvider extends ComponentLocationProvider implements ExceptionContextProvider {



  private static final Pattern URL_PATTERN = compile("url=\"[a-z]*://([^@]*)@");
  private static final Pattern ADDRESS_PATTERN = compile("address=\"[a-z]*://([^@]*)@");
  private static final Pattern PASSWORD_PATTERN = compile("password=\"([^\"|>|\n]*)\"");
  private static final String PASSWORD_MASK = "<<credentials>>";
  public static final String PASSWORD_ATTRIBUTE_MASK = "password=\"%s\"";

  /**
   * Populates the passed beanAnnotations with the other passed parameters.
   *
   * @param beanAnnotations the map with annotations to populate
   * @param sourceCode the source code representation of the element definition.
   * @param customAttributes the custom attributes of the element definition.
   */
  public static void addMetadataAnnotationsFromXml(Map<QName, Object> beanAnnotations, String sourceCode,
                                                   Map<String, Object> customAttributes) {
    if (sourceCode != null) {
      beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, sourceCode);
    }

    String documentationName = (String) customAttributes
        .get(Component.Annotations.NAME_ANNOTATION_KEY.toString());
    if (documentationName != null) {
      beanAnnotations.put(NAME_ANNOTATION_KEY, documentationName);
    }
    customAttributes.forEach((key, value) -> {
      if (!key.equals(NAME_ANNOTATION_KEY.toString())) {
        beanAnnotations.put(QName.valueOf(key), value);
      }
    });
  }

  protected static String getSourceXML(Component element) {
    Object sourceXml = element.getAnnotation(SOURCE_ELEMENT_ANNOTATION_KEY);
    return sourceXml != null ? maskPasswords(sourceXml.toString()) : null;
  }

  public static String maskPasswords(String xml, String passwordMask) {
    xml = maskUrlPassword(xml, URL_PATTERN, passwordMask);
    xml = maskUrlPassword(xml, ADDRESS_PATTERN, passwordMask);

    Matcher matcher = PASSWORD_PATTERN.matcher(xml);
    if (matcher.find() && matcher.groupCount() > 0) {
      xml = xml.replaceAll(maskPasswordAttribute(matcher.group(1)), maskPasswordAttribute(passwordMask));
    }
    xml = maskUrlPassword(xml, PASSWORD_PATTERN, passwordMask);

    return xml;
  }

  protected static String maskPasswords(String xml) {
    return maskPasswords(xml, PASSWORD_MASK);
  }

  private static String maskUrlPassword(String xml, Pattern pattern, String passwordMask) {
    Matcher matcher = pattern.matcher(xml);
    if (matcher.find() && matcher.groupCount() > 0) {
      xml = xml.replaceAll(matcher.group(1), passwordMask);
    }
    return xml;
  }

  private static String maskPasswordAttribute(String password) {
    return format(PASSWORD_ATTRIBUTE_MASK, password);
  }

}
