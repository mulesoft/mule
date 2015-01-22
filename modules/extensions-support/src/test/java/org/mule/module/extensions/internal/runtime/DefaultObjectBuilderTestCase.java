/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.extensions.ExtendedPersonalInfo;
import org.mule.module.extensions.LifetimeInfo;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;
import org.mule.module.extensions.internal.util.ExtensionsTestUtils;
import org.mule.repackaged.internal.org.springframework.util.ReflectionUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
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
    private Method nameSetter;
    private Method ageSetter;
    private Method lifetimeInfoSetter;
    private List<ValueResolver> resolvers = new ArrayList<>();

    @Before
    public void before()
    {
        builder = new DefaultObjectBuilder(PROTOTYPE_CLASS);

        nameSetter = ReflectionUtils.findMethod(PROTOTYPE_CLASS, "setMyName", String.class);
        ageSetter = ReflectionUtils.findMethod(PROTOTYPE_CLASS, "setAge", Integer.class);
        lifetimeInfoSetter = ReflectionUtils.findMethod(PROTOTYPE_CLASS, "setLifetimeInfo", LifetimeInfo.class);
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
        assertThat(personalInfo.getMyName(), is(NAME));
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
        builder.addPropertyResolver(nameSetter, getResolver(NAME, false));
        builder.addPropertyResolver(ageSetter, getResolver(AGE, true));

        assertThat(builder.isDynamic(), is(true));
    }

    @Test
    public void initialise() throws Exception
    {
        builder.setMuleContext(muleContext);
        builder.initialise();
        ExtensionsTestUtils.verifyAllInitialised(resolvers, muleContext);
    }

    @Test
    public void start() throws Exception
    {
        builder.start();
        ExtensionsTestUtils.verifyAllStarted(resolvers);
    }

    @Test
    public void stop() throws Exception
    {
        builder.stop();
        ExtensionsTestUtils.verifyAllStopped(resolvers);
    }

    @Test
    public void dispose() throws Exception
    {
        builder.dispose();
        ExtensionsTestUtils.verifyAllDisposed(resolvers);
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
        builder.addPropertyResolver(nameSetter, getResolver(NAME, dynamic));
        builder.addPropertyResolver(ageSetter, getResolver(AGE, dynamic));
        builder.addPropertyResolver(lifetimeInfoSetter, getResolver(LIFETIME_INFO, dynamic));
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