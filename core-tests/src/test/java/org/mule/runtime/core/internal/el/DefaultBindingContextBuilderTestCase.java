/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.LazyValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import java.util.Collection;
import java.util.Map;

public class DefaultBindingContextBuilderTestCase extends AbstractMuleTestCase {

  private static final String ID = "id";
  private static final String OTHER_ID = "otherId";

  private BindingContext.Builder builder = new DefaultBindingContextBuilder();
  private TypedValue<String> typedValue = new TypedValue<>("", STRING);
  private ModuleNamespace namespace = new ModuleNamespace("org", "mule", "mymodule");

  @Test
  public void addsBinding() {
    BindingContext context = builder.addBinding(ID, typedValue).build();

    assertThat(context.bindings(), hasSize(1));
    assertThat(context.identifiers(), hasItem("id"));
    assertThat(context.lookup("id").get(), is(sameInstance(typedValue)));
  }

  @Test
  public void addsBindings() {
    ExpressionModule module =
        ExpressionModule.builder(namespace).addBinding(ID, typedValue).build();
    BindingContext previousContext =
        BindingContext.builder()
            .addBinding(ID, typedValue)
            .addBinding(OTHER_ID, typedValue)
            .addModule(module)
            .build();


    BindingContext context = builder.addAll(previousContext).build();

    assertThat(context.bindings(), hasSize(2));
    assertThat(context.identifiers(), hasItems(ID, OTHER_ID));
    assertThat(context.lookup(ID).get(), is(sameInstance(typedValue)));
    assertThat(context.lookup(OTHER_ID).get(), is(sameInstance(typedValue)));

    assertThat(context.modules(), hasSize(1));
    assertThat(context.modules(), hasItems(module));
    Collection<Binding> moduleBindings = context.modules().iterator().next().bindings();
    assertThat(moduleBindings, hasSize(1));
    assertThat(moduleBindings.iterator().next().identifier(), is(ID));
  }


  @Test
  public void fromPreviousBindings() {
    ExpressionModule module = ExpressionModule.builder(namespace).addBinding("id", typedValue).build();
    BindingContext previousContext =
        BindingContext.builder()
            .addBinding(ID, typedValue)
            .addBinding(OTHER_ID, typedValue)
            .addModule(module)
            .build();


    BindingContext context = BindingContext.builder(previousContext).build();

    assertThat(context.bindings(), hasSize(2));
    assertThat(context.identifiers(), hasItems(ID, OTHER_ID));
    assertThat(context.lookup(ID).get(), is(sameInstance(typedValue)));
    assertThat(context.lookup(OTHER_ID).get(), is(sameInstance(typedValue)));

    assertThat(context.modules(), hasSize(1));
    assertThat(context.modules(), hasItems(module));
  }

  private static final DataType VARS_DATA_TYPE = DataType.builder()
      .mapType(Map.class)
      .keyType(String.class)
      .valueType(TypedValue.class)
      .build();

  @Test
  public void varLookupInFirstDelegate() {
    BindingContext localBinding = BindingContext.builder()
        .addBinding("vars", new LazyValue<>(() -> new TypedValue<>(singletonMap("key", new TypedValue<>("value", STRING)),
                                                                   VARS_DATA_TYPE)))
        .build();
    BindingContext globalBinding = BindingContext.builder().addBinding("g", new TypedValue<>("gValue", STRING)).build();
    BindingContext composite = BindingContext.builder(localBinding).addAll(globalBinding).build();

    TypedValue vars = composite.lookup("vars").get();
    assertThat(((Map<String, TypedValue<String>>) vars.getValue()).get("key").getValue(), is("value"));
    assertThat(composite.lookup("g").get().getValue(), is("gValue"));
  }

  @Test
  public void varLookupInSecondDelegate() {
    BindingContext localBinding = BindingContext.builder()
        .addBinding("vars", new LazyValue<>(() -> new TypedValue<>(singletonMap("key", new TypedValue<>("value", STRING)),
                                                                   VARS_DATA_TYPE)))
        .build();
    BindingContext globalBinding = BindingContext.builder().addBinding("g", new TypedValue<>("gValue", STRING)).build();
    BindingContext composite = BindingContext.builder(globalBinding).addAll(localBinding).build();

    TypedValue vars = composite.lookup("vars").get();
    assertThat(((Map<String, TypedValue<String>>) vars.getValue()).get("key").getValue(), is("value"));
    assertThat(composite.lookup("g").get().getValue(), is("gValue"));
  }
}
