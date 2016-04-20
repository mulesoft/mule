/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.model.ExtendedPersonalInfo;
import org.mule.test.heisenberg.extension.model.LifetimeInfo;

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
public class DefaultObjectBuilderTestCase extends AbstractMuleTestCase
{

    private static Class<?> PROTOTYPE_CLASS = ExtendedPersonalInfo.class;
    private static final String NAME = "heisenberg";
    private static final int AGE = 50;
    private static LifetimeInfo LIFETIME_INFO = new LifetimeInfo();

    @Mock
    private MuleEvent event;

    @Mock
    private MuleContext muleContext;

    private DefaultObjectBuilder<ExtendedPersonalInfo> builder;
    private Field nameField;
    private Field ageField;
    private Field lifetimeInfoField;
    private List<ValueResolver> resolvers = new ArrayList<>();

    @Before
    public void before()
    {
        builder = new DefaultObjectBuilder(PROTOTYPE_CLASS);

        nameField = getField(PROTOTYPE_CLASS, "name", String.class);
        ageField = getField(PROTOTYPE_CLASS, "age", Integer.class);
        lifetimeInfoField = getField(PROTOTYPE_CLASS, "lifetimeInfo", LifetimeInfo.class);
    }

    @Test
    public void build() throws Exception
    {
        populate(false);
        ExtendedPersonalInfo personalInfo = builder.build(event);
        verify(personalInfo);
    }

    @Test
    public void reusable() throws Exception
    {
        populate(false);
        ExtendedPersonalInfo info1 = builder.build(event);
        ExtendedPersonalInfo info2 = builder.build(event);
        ExtendedPersonalInfo info3 = builder.build(event);

        assertThat(info1, is(not(info2)));
        assertThat(info1, is(not(info3)));
        verify(info1);
        verify(info2);
        verify(info3);
    }

    private void verify(ExtendedPersonalInfo personalInfo)
    {
        assertThat(personalInfo.getName(), is(NAME));
        assertThat(personalInfo.getAge(), is(AGE));
        assertThat(personalInfo.getLifetimeInfo(), is(sameInstance(LIFETIME_INFO)));
    }

    @Test
    public void isStatic() throws Exception
    {
        populate(false);
        assertThat(builder.isDynamic(), is(false));
    }

    @Test
    public void isDynamic() throws Exception
    {
        builder.addPropertyResolver(nameField, getResolver(NAME, false));
        builder.addPropertyResolver(ageField, getResolver(AGE, true));

        assertThat(builder.isDynamic(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildInterface() throws Exception
    {
        builder = new DefaultObjectBuilder(MuleMessage.class);
        builder.build(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void abstractClass() throws Exception
    {
        builder = new DefaultObjectBuilder(TestAbstract.class);
        builder.build(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDefaultConstructor() throws Exception
    {
        builder = new DefaultObjectBuilder(TestNoDefaultConstructor.class);
        builder.build(event);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noPublicConstructor() throws Exception
    {
        builder = new DefaultObjectBuilder(NoPublicConstructor.class);
        builder.build(event);
    }

    private void populate(boolean dynamic) throws Exception
    {
        builder.addPropertyResolver(nameField, getResolver(NAME, dynamic));
        builder.addPropertyResolver(ageField, getResolver(AGE, dynamic));
        builder.addPropertyResolver(lifetimeInfoField, getResolver(LIFETIME_INFO, dynamic));
    }

    private ValueResolver getResolver(Object value, boolean dynamic) throws Exception
    {
        ValueResolver resolver = ExtensionsTestUtils.getResolver(value, event, dynamic);
        resolvers.add(resolver);

        return resolver;
    }

    private static abstract class TestAbstract
    {

    }

    public static class TestNoDefaultConstructor
    {

        public TestNoDefaultConstructor(String value)
        {
        }
    }

    public static class NoPublicConstructor
    {

        protected NoPublicConstructor()
        {
        }
    }
}