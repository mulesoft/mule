/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.model.PersonalInfo;

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
    private ParameterModel parameterModel;

    @Mock
    private ResolverSetResult result;


    private ValueSetter valueSetter;

    @Before
    public void before()
    {
        when(result.get(parameterModel)).thenReturn(NAME);
        valueSetter = new SingleValueSetter(parameterModel, getField(PersonalInfo.class, "name", String.class));
    }

    @Test
    public void set() throws Exception
    {
        PersonalInfo info = new PersonalInfo();
        valueSetter.set(info, result);
        assertThat(info.getName(), is(NAME));
    }
}
