/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.el.v2.DefaultBindingContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class DefaultBindingContextBuilderTestCase extends AbstractMuleTestCase {

  private static final String ID = "id";
  private static final String OTHER_ID = "otherId";

  private BindingContext.Builder builder = new DefaultBindingContextBuilder();
  private TypedValue<String> typedValue = new TypedValue<>("", STRING);

  @Test
  public void addsBinding() {
    BindingContext context = builder.addBinding(ID, typedValue).build();

    assertThat(context.bindings(), hasSize(1));
    assertThat(context.identifiers(), hasItem("id"));
    assertThat(context.lookup("id").get(), is(sameInstance(typedValue)));
  }

  @Test
  public void addsBindings() {
    BindingContext previousContext =
        BindingContext.builder().addBinding(ID, typedValue).addBinding(OTHER_ID, typedValue).build();
    BindingContext context = builder.addAll(previousContext).build();

    assertThat(context.bindings(), hasSize(2));
    assertThat(context.identifiers(), hasItems(ID, OTHER_ID));
    assertThat(context.lookup(ID).get(), is(sameInstance(typedValue)));
    assertThat(context.lookup(OTHER_ID).get(), is(sameInstance(typedValue)));
  }

}
