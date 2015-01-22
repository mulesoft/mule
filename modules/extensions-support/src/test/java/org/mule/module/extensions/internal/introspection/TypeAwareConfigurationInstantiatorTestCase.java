/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.extensions.introspection.ConfigurationInstantiator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

@SmallTest
public class TypeAwareConfigurationInstantiatorTestCase extends AbstractMuleTestCase
{

    private ConfigurationInstantiator instantiator;

    @Test
    public void instantiate()
    {
        instantiator = new TypeAwareConfigurationInstantiator(Apple.class);
        Object object = instantiator.newInstance();
        assertThat(object, instanceOf(Apple.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDefaultConstructor()
    {
        instantiator = new TypeAwareConfigurationInstantiator(TypeAwareConfigurationInstantiator.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullType()
    {
        instantiator = new TypeAwareConfigurationInstantiator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void interfaceType()
    {
        instantiator = new TypeAwareConfigurationInstantiator(MuleMessage.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void abstractClass()
    {
        instantiator = new TypeAwareConfigurationInstantiator(AbstractMuleTestCase.class);
    }
}
