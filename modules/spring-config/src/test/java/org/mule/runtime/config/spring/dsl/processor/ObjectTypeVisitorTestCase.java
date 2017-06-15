/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.processor;

import static org.junit.Assert.assertTrue;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.dsl.api.component.TypeDefinition;

import org.junit.Test;

public class ObjectTypeVisitorTestCase {

  private static final ObjectTypeVisitor visitor = new ObjectTypeVisitor(new ComponentModel());

  @Test
  public void typeIsInstanceOfGivenClass() {

    TypeDefinition typeDefinition = fromType(String.class);
    typeDefinition.visit(this.visitor);
    assertTrue(String.class.isAssignableFrom(this.visitor.getType()));

  }


}
