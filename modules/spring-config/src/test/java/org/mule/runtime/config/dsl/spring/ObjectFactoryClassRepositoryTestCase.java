/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.spring;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository;
import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.SmartFactoryBean;

public class ObjectFactoryClassRepositoryTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Test
  public void cacheEnableForCGLib()
      throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
    final ComponentBuildingDefinition componentBuildingDefinition = mock(ComponentBuildingDefinition.class);
    Class<ObjectFactory> objectFactoryClass =
        getObjectFactoryClass(componentBuildingDefinition, FakeObjectConnectionProviderObjectFactory.class);

    // First we create the objectFactory that will create an Enhancer with the callback registered and using internal cache
    ObjectFactory objectFactory = objectFactoryClass.newInstance();
    assertThat(((SmartFactoryBean) objectFactory).isSingleton(), is(true));
    Class firstProxyGenerated = objectFactory.getClass();

    objectFactoryClass = getObjectFactoryClass(componentBuildingDefinition, FakeObjectConnectionProviderObjectFactory.class);
    objectFactory = objectFactoryClass.newInstance();
    assertThat(((SmartFactoryBean) objectFactory).isSingleton(), is(true));

    assertThat(firstProxyGenerated, sameInstance(objectFactory.getClass()));
  }

  @Test
  public void compositeClassLoadersAreCorrectlyCached() throws Exception {
    final ComponentBuildingDefinition componentBuildingDefinition = mock(ComponentBuildingDefinition.class);

    String classpath = System.getProperty("java.class.path");
    String[] entries = classpath.split(File.pathSeparator);
    URL[] unitTestClassLoaderUrls = new URL[entries.length];
    for (int i = 0; i < entries.length; i++) {
      unitTestClassLoaderUrls[i] = Paths.get(entries[i]).toAbsolutePath().toUri().toURL();
    }

    ClassLoader classLoader1 = new URLClassLoader(unitTestClassLoaderUrls,
                                                  this.getClass().getClassLoader().getParent());
    ClassLoader classLoader2 = new URLClassLoader(unitTestClassLoaderUrls,
                                                  this.getClass().getClassLoader().getParent());

    Class factoryClass1 = classLoader1.loadClass(FakeObjectConnectionProviderObjectFactory.class.getName());
    Class factoryClass2 = classLoader2.loadClass(FakeObjectConnectionProviderObjectFactory.class.getName());
    Class otherFactoryClass1 = classLoader1.loadClass(OtherFakeObjectConnectionProviderObjectFactory.class.getName());
    Class otherFactoryClass2 = classLoader2.loadClass(OtherFakeObjectConnectionProviderObjectFactory.class.getName());

    Class<ObjectFactory> enhancedFactoryClass1 = getObjectFactoryClass(componentBuildingDefinition, factoryClass1);
    Class<ObjectFactory> enhancedFactoryClass2 = getObjectFactoryClass(componentBuildingDefinition, factoryClass2);
    Class<ObjectFactory> enhancedOtherFactoryClass1 = getObjectFactoryClass(componentBuildingDefinition, otherFactoryClass1);
    Class<ObjectFactory> enhancedOtherFactoryClass2 = getObjectFactoryClass(componentBuildingDefinition, otherFactoryClass2);

    assertThat(enhancedFactoryClass1.getClassLoader(), instanceOf(CompositeClassLoader.class));
    assertThat(enhancedFactoryClass2.getClassLoader(), instanceOf(CompositeClassLoader.class));
    assertThat(enhancedOtherFactoryClass1.getClassLoader(), instanceOf(CompositeClassLoader.class));
    assertThat(enhancedOtherFactoryClass2.getClassLoader(), instanceOf(CompositeClassLoader.class));

    assertThat(enhancedFactoryClass1.getClassLoader(), is(sameInstance(enhancedOtherFactoryClass1.getClassLoader())));
    assertThat(enhancedFactoryClass2.getClassLoader(), is(sameInstance(enhancedOtherFactoryClass2.getClassLoader())));
    assertThat(enhancedFactoryClass1.getClassLoader(), is(not(sameInstance(enhancedFactoryClass2.getClassLoader()))));
    assertThat(enhancedOtherFactoryClass1.getClassLoader(), is(not(sameInstance(enhancedOtherFactoryClass2.getClassLoader()))));
  }

  public Class<ObjectFactory> getObjectFactoryClass(ComponentBuildingDefinition componentBuildingDefinition,
                                                    Class factoryType) {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();
    return objectFactoryClassRepository.getObjectFactoryClass(componentBuildingDefinition,
                                                              factoryType,
                                                              FakeObject.class, () -> false);
  }


  public static class FakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory {

    @Override
    public Object doGetObject() throws Exception {
      return new FakeObject();
    }

  }

  public static class OtherFakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory {

    @Override
    public Object doGetObject() throws Exception {
      return new FakeObject();
    }

  }

  public static class FakeObject {
  }

}
