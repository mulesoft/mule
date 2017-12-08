/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.extension.internal.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.List;

public class ConnectionProviderASTElement extends ASTType implements ConnectionProviderElement {

  public ConnectionProviderASTElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    super(typeElement, processingEnvironment);
  }

  public List<org.mule.runtime.module.extension.internal.loader.java.type.Type> getInterfaceGenerics(Class clazz) {
    return IntrospectionUtils
        .getInterfaceGenerics(typeElement, processingEnvironment.getElementUtils().getTypeElement(clazz.getName()),
                              processingEnvironment)
        .stream()
        .map(typeMirror -> new ASTType(typeMirror, processingEnvironment))
        .collect(toList());
  }

}
