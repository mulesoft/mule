/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultObjectBuilderTestCase extends AbstractMuleTestCase {

  private static Class<?> PROTOTYPE_CLASS = PersonalInfo.class;
  private static final String NAME = "heisenberg";
  private static final int AGE = 50;

  @Mock
  private CoreEvent event;

  @Mock
  private ValueResolvingContext resolvingContext;

  @Mock
  private MuleContext muleContext;

  private DefaultObjectBuilder<PersonalInfo> builder;
  private Field nameField;
  private Field ageField;
  private Field lifetimeInfoField;
  private List<ValueResolver> resolvers = new ArrayList<>();

  @Before
  public void before() {
    builder = new DefaultObjectBuilder(PROTOTYPE_CLASS);

    nameField = getField(PROTOTYPE_CLASS, "name").get();
    ageField = getField(PROTOTYPE_CLASS, "age").get();

    when(resolvingContext.getEvent()).thenReturn(event);
    when(resolvingContext.getConfig()).thenReturn(empty());
  }

  @Test
  public void build() throws Exception {
    populate(false);
    PersonalInfo personalInfo = builder.build(resolvingContext);
    verify(personalInfo);
  }

  @Test
  public void reusable() throws Exception {
    populate(false);
    PersonalInfo info1 = builder.build(resolvingContext);
    PersonalInfo info2 = builder.build(resolvingContext);
    PersonalInfo info3 = builder.build(resolvingContext);

    assertThat(info1, is(not(sameInstance(info2))));
    assertThat(info1, is(not(sameInstance(info3))));
    verify(info1);
    verify(info2);
    verify(info3);
  }

  private void verify(PersonalInfo personalInfo) {
    assertThat(personalInfo.getName(), is(NAME));
    assertThat(personalInfo.getAge(), is(AGE));
  }

  @Test
  public void isStatic() throws Exception {
    populate(false);
    assertThat(builder.isDynamic(), is(false));
  }

  @Test
  public void isDynamic() throws Exception {
    builder.addPropertyResolver(nameField.getName(), getResolver(NAME, false));
    builder.addPropertyResolver(ageField.getName(), getResolver(AGE, true));

    assertThat(builder.isDynamic(), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void buildInterface() throws Exception {
    builder = new DefaultObjectBuilder(InternalMessage.class);
    builder.build(resolvingContext);
  }

  @Test(expected = IllegalArgumentException.class)
  public void abstractClass() throws Exception {
    builder = new DefaultObjectBuilder(TestAbstract.class);
    builder.build(resolvingContext);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noDefaultConstructor() throws Exception {
    builder = new DefaultObjectBuilder(TestNoDefaultConstructor.class);
    builder.build(resolvingContext);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noPublicConstructor() throws Exception {
    builder = new DefaultObjectBuilder(NoPublicConstructor.class);
    builder.build(resolvingContext);
  }

  private void populate(boolean dynamic) throws Exception {
    builder.addPropertyResolver(nameField.getName(), getResolver(NAME, dynamic));
    builder.addPropertyResolver(ageField.getName(), getResolver(AGE, dynamic));
  }

  private ValueResolver getResolver(Object value, boolean dynamic) throws Exception {
    ValueResolver resolver = ExtensionsTestUtils.getResolver(value, resolvingContext, dynamic);
    resolvers.add(resolver);

    return resolver;
  }

  private static abstract class TestAbstract {

  }

  public static class TestNoDefaultConstructor {

    public TestNoDefaultConstructor(String value) {}
  }

  public static class NoPublicConstructor {

    protected NoPublicConstructor() {}
  }
}
