/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.STRING;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultExpressionModuleBuilderTestCase extends AbstractMuleTestCase {

  public static final String ID = "id";

  private TypedValue<String> typedValue = new TypedValue<>("", STRING);
  private ModuleNamespace namespace = new ModuleNamespace("org", "mule", "mymodule");

  @Test
  public void shouldCreateTheModuleCorrectly() {

    ExpressionModule.Builder builder = ExpressionModule.builder(namespace);
    builder.addBinding(ID, typedValue);

    ExpressionModule module = builder.build();
    assertThat(module.bindings(), hasSize(1));
    assertThat(module.identifiers(), hasItem(ID));
    assertThat(module.lookup(ID).get(), is(sameInstance(typedValue)));
    assertThat(module.namespace(), is(namespace));

  }

  @Test
  public void shouldBeAddedToBindingContext() {
    BindingContext.Builder bindingContextBuilder = BindingContext.builder();
    ExpressionModule.Builder builder = ExpressionModule.builder(namespace);
    builder.addBinding("id", typedValue);
    ExpressionModule module = builder.build();
    bindingContextBuilder.addModule(module);

    BindingContext bindingContext = bindingContextBuilder.build();
    assertThat(bindingContext.modules(), hasSize(1));
    assertThat(bindingContext.modules(), hasItem(module));
  }

}
