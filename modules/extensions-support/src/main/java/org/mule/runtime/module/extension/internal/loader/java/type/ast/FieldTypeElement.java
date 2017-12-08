/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import java.lang.reflect.Field;


/**
 * @since 4.1
 */
public class FieldTypeElement extends VariableElementAST implements FieldElement {

  public FieldTypeElement(VariableElement elem, ProcessingEnvironment processingEnvironment) {
    super(elem, processingEnvironment);
  }

  @Override
  public Field getField() {
    return null;
  }

  @Override
  public String getOwnerDescription() {
    return "Class";
  }

}
