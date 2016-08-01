/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergSource;

import java.lang.reflect.InvocationTargetException;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultSourceFactoryTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void create()
    {
        assertThat(new DefaultSourceFactory(HeisenbergSource.class).createSource(), is(instanceOf(HeisenbergSource.class)));
    }

    @Test
    public void nullType()
    {
        expectedException.expect(IllegalArgumentException.class);
        new DefaultSourceFactory(null);
    }

    @Test
    public void notInstantiable()
    {
        expectedException.expect(IllegalArgumentException.class);
        new DefaultSourceFactory(Source.class);
    }

    @Test
    public void exceptionOnInstantiation()
    {
        expectedException.expect(RuntimeException.class);
        expectedException.expectCause(Matchers.instanceOf(InvocationTargetException.class));
        new DefaultSourceFactory(UncreatableSource.class).createSource();
    }

    public static class UncreatableSource extends Source
    {

        public UncreatableSource()
        {
            throw new IllegalArgumentException();
        }

        @Override
        public void start()
        {

        }

        @Override
        public void stop()
        {

        }
    }
}
