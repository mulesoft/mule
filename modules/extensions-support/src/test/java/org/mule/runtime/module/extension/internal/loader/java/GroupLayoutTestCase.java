/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.test.vegan.extension.SpreadVeganismOperation.ARGUMENTS_TAB;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.test.vegan.extension.VeganExtension;

import org.junit.Before;
import org.junit.Test;

public class GroupLayoutTestCase extends AbstractJavaExtensionDeclarationTestCase {

  private ExtensionDeclaration extensionDeclaration;

  @Before
  public void setUp() {
    setLoader(loaderFor(VeganExtension.class));
    extensionDeclaration = declareExtension().getDeclaration();
  }

  @Test
  public void groupLayout() {
    OperationDeclaration operation = getOperation(extensionDeclaration, "convinceAnimalKiller");
    assertThat(operation.getParameterGroups(), hasSize(1));
    ParameterGroupDeclaration group = operation.getParameterGroups().get(0);
    assertThat(group.getLayoutModel().getTabName().get(), is(ARGUMENTS_TAB));
  }

}
