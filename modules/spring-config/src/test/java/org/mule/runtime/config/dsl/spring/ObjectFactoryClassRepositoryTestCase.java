/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.spring;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.SmartFactoryBean;

@RunWith(MockitoJUnitRunner.class)
public class ObjectFactoryClassRepositoryTestCase {

  @Mock
  private ComponentBuildingDefinition componentBuildingDefinition;

  @Test
  public void cacheEnableForCGLib()
      throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
    Class<ObjectFactory> objectFactoryClass = getObjectFactoryClass();

    // First we create the objectFactory that will create an Enhancer with the callback registered and using internal cache
    ObjectFactory objectFactory = objectFactoryClass.newInstance();
    assertThat(((SmartFactoryBean) objectFactory).isSingleton(), is(true));
    Class firstProxyGenerated = objectFactory.getClass();

    objectFactoryClass = getObjectFactoryClass();
    objectFactory = objectFactoryClass.newInstance();
    assertThat(((SmartFactoryBean) objectFactory).isSingleton(), is(true));

    assertThat(firstProxyGenerated, sameInstance(objectFactory.getClass()));
  }

  public Class<ObjectFactory> getObjectFactoryClass() {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();
    return objectFactoryClassRepository.getObjectFactoryClass(componentBuildingDefinition,
                                                              FakeObjectConnectionProviderObjectFactory.class,
                                                              FakeObject.class, () -> false, empty());
  }


  public static class FakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory {

    @Override
    public Object doGetObject() throws Exception {
      return new FakeObject();
    }

  }

  public static class FakeObject {
  }

}
