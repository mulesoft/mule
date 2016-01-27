/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.extension.api.introspection.ConfigurationFactory;
import org.mule.module.extension.internal.introspection.describer.TypeAwareConfigurationFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

@SmallTest
public class TypeAwareConfigurationFactoryTestCase extends AbstractMuleTestCase
{

    private ConfigurationFactory instantiator;

    @Test
    public void instantiate()
    {
        instantiator = new TypeAwareConfigurationFactory(Apple.class);
        Object object = instantiator.newInstance();
        assertThat(object, instanceOf(Apple.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDefaultConstructor()
    {
        instantiator = new TypeAwareConfigurationFactory(TypeAwareConfigurationFactory.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullType()
    {
        instantiator = new TypeAwareConfigurationFactory(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void interfaceType()
    {
        instantiator = new TypeAwareConfigurationFactory(MuleMessage.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void abstractClass()
    {
        instantiator = new TypeAwareConfigurationFactory(AbstractMuleTestCase.class);
    }
}
