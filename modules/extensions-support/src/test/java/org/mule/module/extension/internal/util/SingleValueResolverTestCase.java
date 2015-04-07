/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.PersonalInfo;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SingleValueResolverTestCase extends AbstractMuleTestCase
{

    private static final String NAME = "name";

    @Mock
    private Parameter parameter;

    @Mock
    private ResolverSetResult result;


    private ValueSetter valueSetter;

    @Before
    public void before()
    {
        when(result.get(parameter)).thenReturn(NAME);
        valueSetter = new SingleValueSetter(parameter, getField(PersonalInfo.class, "name", String.class));
    }

    @Test
    public void set() throws Exception
    {
        PersonalInfo info = new PersonalInfo();
        valueSetter.set(info, result);
        assertThat(info.getName(), is(NAME));
    }
}
