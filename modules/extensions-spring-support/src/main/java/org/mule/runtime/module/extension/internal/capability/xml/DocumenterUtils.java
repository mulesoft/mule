/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.capability.xml.schema.ExtensionAnnotationProcessor;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Helper class used by the documenter to introspect properties of the extension source code.
 *
 * @since 4.5
 */
public class DocumenterUtils {

  private DocumenterUtils() {}

  public static boolean isParameterGroup(VariableElement variableElement) {
    return variableElement.getAnnotation(ParameterGroup.class) != null
        || variableElement.getAnnotation(org.mule.sdk.api.annotation.param.ParameterGroup.class) != null;
  }

  public static Map<String, VariableElement> getParameters(TypeElement typeElement,
                                                           ExtensionAnnotationProcessor extensionAnnotationProcessor) {
    Map<String, VariableElement> parameterFields =
        new HashMap<>(extensionAnnotationProcessor.getFieldsAnnotatedWith(typeElement, Parameter.class));
    parameterFields.putAll(extensionAnnotationProcessor
        .getFieldsAnnotatedWith(typeElement, org.mule.sdk.api.annotation.param.Parameter.class));
    return parameterFields;
  }

  public static Map<String, VariableElement> getParameterGroups(TypeElement typeElement,
                                                                ExtensionAnnotationProcessor extensionAnnotationProcessor) {
    Map<String, VariableElement> parameterGroupFields =
        new HashMap<>(extensionAnnotationProcessor.getFieldsAnnotatedWith(typeElement, ParameterGroup.class));
    parameterGroupFields.putAll(extensionAnnotationProcessor
        .getFieldsAnnotatedWith(typeElement, org.mule.sdk.api.annotation.param.ParameterGroup.class));
    return parameterGroupFields;
  }

  public static boolean isParameterGroupAnnotation(Class clazz) {
    return ParameterGroup.class.isAssignableFrom(clazz)
        || org.mule.sdk.api.annotation.param.ParameterGroup.class.isAssignableFrom(clazz);
  }

  public static boolean isOperationTransactionalActionType(String typeName) {
    return OperationTransactionalAction.class.getName().equals(typeName)
        || org.mule.sdk.api.tx.OperationTransactionalAction.class.getName().equals(typeName);
  }
}
