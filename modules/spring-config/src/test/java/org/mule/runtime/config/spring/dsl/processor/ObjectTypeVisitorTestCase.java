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


  @Test
  public void typeIsInstanceOfGivenClass() {

    ObjectTypeVisitor visitor = new ObjectTypeVisitor(new ComponentModel());
    TypeDefinition typeDefinition = fromType(String.class);
    typeDefinition.visit(visitor);
    assertTrue(String.class.isAssignableFrom(visitor.getType()));

  }

  @Test
  public void typeIsInstanceOfGivenClassFromAttibute() throws ClassNotFoundException {
    Thread.currentThread().getContextClassLoader().loadClass("org.mule.runtime.core.api.processor.MessageProcessors");
    ComponentModel componentModel = new ComponentModel();
    componentModel.setParameter("parentClass", "org.mule.runtime.core.api.processor.MessageProcessors");
    ObjectTypeVisitor visitor = new ObjectTypeVisitor(componentModel);
    TypeDefinition typeDefinition = fromConfigurationAttribute("parentClass");
    typeDefinition.visit(visitor);
    assertTrue(org.mule.runtime.core.api.processor.MessageProcessors.class.isAssignableFrom(visitor.getType()));
  }


}

