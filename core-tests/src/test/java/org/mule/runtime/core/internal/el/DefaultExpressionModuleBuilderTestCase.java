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
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import java.util.List;

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

  @Test
  public void shouldBeAbleToAddDeclaredTypes() {
    ExpressionModule.Builder builder = ExpressionModule.builder(namespace);
    NumberType numberType = create(MetadataFormat.JSON).numberType().build();
    ObjectTypeBuilder objectTypeBuilder = create(MetadataFormat.JSON).objectType();
    objectTypeBuilder.addField().key("age").value(numberType).build();
    ObjectType objectType = objectTypeBuilder.build();
    builder.addType(objectType);
    ExpressionModule module = builder.build();

    List<MetadataType> declaredTypes = module.declaredTypes();
    assertThat(declaredTypes, hasSize(1));
    MetadataType declaredType = declaredTypes.get(0);
    assertThat(declaredType, is(sameInstance(objectType)));
    assertThat(((ObjectType) declaredType).getFieldByName("age").get().getValue(), is(sameInstance((numberType))));
  }

}
