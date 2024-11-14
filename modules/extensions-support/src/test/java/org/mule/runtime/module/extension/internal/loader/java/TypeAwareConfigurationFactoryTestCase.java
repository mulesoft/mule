/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

@SmallTest
public class TypeAwareConfigurationFactoryTestCase extends AbstractMuleTestCase {

  @Test
  public void instantiate() {
    ConfigurationFactory instantiator = new TypeAwareConfigurationFactory(Apple.class, Apple.class.getClassLoader(), true);
    Object object = instantiator.newInstance();
    assertThat(object, instanceOf(Apple.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void noDefaultConstructor() {
    new TypeAwareConfigurationFactory(TypeAwareConfigurationFactory.class,
                                      TypeAwareConfigurationFactory.class.getClassLoader(), true);
  }

  @Test(expected = NullPointerException.class)
  public void nullType() {
    new TypeAwareConfigurationFactory(null, getClass().getClassLoader(), true);
  }


  @Test(expected = NullPointerException.class)
  public void nullClassLoader() {
    new TypeAwareConfigurationFactory(Apple.class, null, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void interfaceType() {
    new TypeAwareConfigurationFactory(InternalMessage.class, getClass().getClassLoader(), true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void abstractClass() {
    new TypeAwareConfigurationFactory(AbstractMuleTestCase.class, getClass().getClassLoader(), true);
  }
}
