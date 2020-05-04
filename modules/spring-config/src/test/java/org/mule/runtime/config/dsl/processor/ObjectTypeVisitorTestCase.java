/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.dsl.processor;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel.Builder;
import org.mule.runtime.core.internal.processor.AbstractProcessor;
import org.mule.runtime.core.internal.processor.ReferenceProcessor;
import org.mule.runtime.dsl.api.component.TypeDefinition;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ObjectTypeVisitorTestCase {

  @BeforeClass
  public static void loadClassLoader() throws ClassNotFoundException {
    Thread.currentThread().getContextClassLoader().loadClass("org.mule.runtime.core.internal.processor.ReferenceProcessor");
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();


  @Test
  public void typeIsInstanceOfGivenClass() {
    ObjectTypeVisitor visitor = new ObjectTypeVisitor((ComponentAst) baseComponentModelBuilder().build());
    TypeDefinition typeDefinition = fromType(String.class);
    typeDefinition.visit(visitor);
    assertTrue(String.class.isAssignableFrom(visitor.getType()));

  }

  @Test
  public void typeIsInstanceOfGivenClassFromAttribute() throws ClassNotFoundException {
    ObjectTypeVisitor visitor = new ObjectTypeVisitor((ComponentAst) baseComponentModelBuilder()
        .addParameter("type", "org.mule.runtime.core.internal.processor.ReferenceProcessor", false).build());
    TypeDefinition typeDefinition = fromConfigurationAttribute("type");
    typeDefinition.visit(visitor);
    assertTrue(ReferenceProcessor.class.isAssignableFrom(visitor.getType()));
  }

  @Test
  public void typeIsInstanceOfCheckedClassFromAttribute() throws ClassNotFoundException {
    ObjectTypeVisitor visitor = new ObjectTypeVisitor((ComponentAst) baseComponentModelBuilder()
        .addParameter("type", "org.mule.runtime.core.internal.processor.ReferenceProcessor", false).build());
    TypeDefinition typeDefinition = fromConfigurationAttribute("type")
        .checkingThatIsClassOrInheritsFrom(ReferenceProcessor.class);
    typeDefinition.visit(visitor);
    assertTrue(ReferenceProcessor.class.isAssignableFrom(visitor.getType()));
  }

  @Test
  public void typeIsInstanceOfClassInheritedFromCheckedClassFromAttribute() throws ClassNotFoundException {
    ObjectTypeVisitor visitor = new ObjectTypeVisitor((ComponentAst) baseComponentModelBuilder()
        .addParameter("type", "org.mule.runtime.core.internal.processor.ReferenceProcessor", false).build());
    // Check that ReferenceProcessor inherits from AbstractProcessor
    TypeDefinition typeDefinition = fromConfigurationAttribute("type")
        .checkingThatIsClassOrInheritsFrom(AbstractProcessor.class);
    typeDefinition.visit(visitor);
    assertTrue(AbstractProcessor.class.isAssignableFrom(visitor.getType()));
  }

  @Test
  public void testFailsIfTypeIsNotOfCheckedClass() throws ClassNotFoundException {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage("is not the same nor inherits from");
    ObjectTypeVisitor visitor = new ObjectTypeVisitor((ComponentAst) baseComponentModelBuilder()
        .addParameter("type", this.getClass().getName(), false).build());
    TypeDefinition typeDefinition = fromConfigurationAttribute("type")
        .checkingThatIsClassOrInheritsFrom(ReferenceProcessor.class);
    typeDefinition.visit(visitor);
  }

  private Builder baseComponentModelBuilder() {
    return new ComponentModel.Builder().setIdentifier(mock(ComponentIdentifier.class));
  }

}

