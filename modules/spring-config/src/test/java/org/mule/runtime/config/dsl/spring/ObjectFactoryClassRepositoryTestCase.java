/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.spring;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_BYTE_BUDDY_OBJECT_CREATION_PROPERTY;

import io.qameta.allure.Issue;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;
import org.mule.tck.junit4.rule.SystemProperty;

@Issue("W-10672687")
public class ObjectFactoryClassRepositoryTestCase {

  @Rule
  public MockitoRule rule = rule();

  @Rule
  public SystemProperty enableByteBuddy = new SystemProperty(ENABLE_BYTE_BUDDY_OBJECT_CREATION_PROPERTY, "true");

  @Test
  public void testSetters() throws InstantiationException, IllegalAccessException {

    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    ObjectFactoryClassRepository.SmartFactoryBeanInterceptor byteBuddyClass =
        (ObjectFactoryClassRepository.SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, false).newInstance();

    byteBuddyClass.setIsSingleton(true);
    byteBuddyClass.setObjectTypeClass(String.class);
    byteBuddyClass.setIsPrototype(true);
    byteBuddyClass.setIsEagerInit(new LazyValue<>(() -> true));

    assertThat(byteBuddyClass.isSingleton(), is(true));
    assertThat(byteBuddyClass.getObjectType(), is(String.class));
    assertThat(byteBuddyClass.isPrototype(), is(true));
    assertThat(byteBuddyClass.isEagerInit(), is(true));

    byteBuddyClass.setIsSingleton(false);
    byteBuddyClass.setObjectTypeClass(Integer.class);
    byteBuddyClass.setIsPrototype(false);
    byteBuddyClass.setIsEagerInit(new LazyValue<>(() -> false));

    assertThat(byteBuddyClass.isSingleton(), is(false));
    assertThat(byteBuddyClass.getObjectType(), is(Integer.class));
    assertThat(byteBuddyClass.isPrototype(), is(false));
    assertThat(byteBuddyClass.isEagerInit(), is(false));
  }

  @Test
  public void testLoadSameClassHasDifferentInterceptors() throws InstantiationException, IllegalAccessException {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    ObjectFactoryClassRepository.SmartFactoryBeanInterceptor byteBuddyClass =
        (ObjectFactoryClassRepository.SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, false).newInstance();
    byteBuddyClass.setObjectTypeClass(String.class);

    ObjectFactoryClassRepository.SmartFactoryBeanInterceptor otherByteBuddyClass =
        (ObjectFactoryClassRepository.SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, false).newInstance();
    otherByteBuddyClass.setObjectTypeClass(Integer.class);

    assertThat(byteBuddyClass.getClass(), is(otherByteBuddyClass.getClass()));
    assertThat(byteBuddyClass.getObjectType(), is(String.class));
    assertThat(otherByteBuddyClass.getObjectType(), is(Integer.class));
  }

  @Test
  public void testGetObjectTypeReturnsSuperIfImplementsObjectTypeProvider()
      throws InstantiationException, IllegalAccessException {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    ObjectFactoryClassRepository.SmartFactoryBeanInterceptor byteBuddyClass =
        (ObjectFactoryClassRepository.SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(OtherFakeObjectConnectionProviderObjectFactory.class, false).newInstance();

    byteBuddyClass.setObjectTypeClass(String.class);
    assertThat(byteBuddyClass.getObjectType(), is(Long.class));
  }

  public static class FakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory {

    @Override
    public Object doGetObject() throws Exception {
      return new FakeObject();
    }

  }

  public static class OtherFakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory
      implements ObjectTypeProvider {

    @Override
    public Object doGetObject() throws Exception {
      return new FakeObject();
    }

    @Override
    public Class<?> getObjectType() {
      return Long.class;
    }
  }

  public static class FakeObject {
  }

}
