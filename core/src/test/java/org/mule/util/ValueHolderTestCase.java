/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ValueHolderTestCase extends AbstractMuleTestCase
{

    private static final String VALUE = "Hello World!";

    private ValueHolder<String> valueHolder;

    @Before
    public void before()
    {
        valueHolder = new ValueHolder<>();
    }

    @Test
    public void defaultValue()
    {
        assertThat(valueHolder.get(), is(nullValue()));
    }

    @Test
    public void initialValue()
    {
        valueHolder = new ValueHolder<>(VALUE);
        assertThat(valueHolder.get(), is(VALUE));
    }

    @Test
    public void setAndGet()
    {
        assertThat(valueHolder.set(VALUE), is(nullValue()));
        assertThat(valueHolder.get(), is(VALUE));
    }

    @Test
    public void overrideValue()
    {
        assertThat(valueHolder.set(VALUE), is(nullValue()));
        assertThat(valueHolder.set(EMPTY), is(VALUE));
        assertThat(valueHolder.get(), is(EMPTY));
    }

    @Test
    public void nullify()
    {
        assertThat(valueHolder.set(VALUE), is(nullValue()));
        assertThat(valueHolder.set(null), is(VALUE));
        assertThat(valueHolder.get(), is(nullValue()));
    }
}
