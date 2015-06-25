/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.extension.introspection.declaration.DescribingContext;
import org.mule.extension.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultDescribingContextTestCase extends AbstractMuleTestCase
{

    private DeclarationDescriptor descriptor;

    private DescribingContext context;

    @Before
    public void before()
    {
        descriptor = new DeclarationDescriptor("name", "version");
        context = new DefaultDescribingContext(descriptor);
    }

    @Test
    public void getExtensionType()
    {
        assertThat(descriptor, is(sameInstance(context.getDeclarationDescriptor())));
    }

    @Test
    public void customParameters()
    {
        assertThat(context.getCustomParameters(), is(notNullValue()));
        assertThat(context.getCustomParameters().isEmpty(), is(true));

        final String key = "key";
        final String value = "value";

        context.getCustomParameters().put(key, value);
        assertThat(context.getCustomParameters().values(), hasSize(1));
        assertThat(context.getCustomParameters().get(key), is(sameInstance((Object) value)));
    }
}
