/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.spring;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.junit.MockitoJUnit.rule;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository;
import org.mule.runtime.config.privileged.dsl.spring.SmartFactoryBeanInterceptor;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;

import org.springframework.beans.factory.SmartFactoryBean;

import org.junit.Rule;
import org.junit.Test;

import org.mockito.junit.MockitoRule;

import io.qameta.allure.Issue;

@Issue("W-10672687")
public class ObjectFactoryClassRepositoryTestCase {

  @Rule
  public MockitoRule rule = rule();

  @Test
  public void testSetters() throws Exception {

    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    SmartFactoryBeanInterceptor byteBuddyClass =
        (SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, String.class).newInstance();
    final SmartFactoryBean bbAsSmf = (SmartFactoryBean) byteBuddyClass;

    byteBuddyClass.setIsSingleton(true);
    byteBuddyClass.setIsPrototype(true);
    byteBuddyClass.setIsEagerInit(new LazyValue<>(() -> true));

    assertThat(bbAsSmf.isSingleton(), is(true));
    assertThat(bbAsSmf.getObjectType(), is(String.class));
    assertThat(bbAsSmf.isPrototype(), is(true));
    assertThat(bbAsSmf.isEagerInit(), is(true));

    byteBuddyClass.setIsSingleton(false);
    byteBuddyClass.setIsPrototype(false);
    byteBuddyClass.setIsEagerInit(new LazyValue<>(() -> false));

    assertThat(bbAsSmf.isSingleton(), is(false));
    assertThat(bbAsSmf.isPrototype(), is(false));
    assertThat(bbAsSmf.isEagerInit(), is(false));
    assertThat(bbAsSmf.getObject(), is(instanceOf(FakeObject.class)));
  }

  @Test
  @Issue("W-12362157")
  public void getObjectTypeWithoutInitializingTheFields() throws InstantiationException, IllegalAccessException {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    SmartFactoryBeanInterceptor byteBuddyClass =
        (SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, String.class).newInstance();
    final SmartFactoryBean bbAsSmf = (SmartFactoryBean) byteBuddyClass;

    assertThat(bbAsSmf.getObjectType(), is(String.class));
  }

  @Test
  @Issue("W-12362157")
  public void testSameClassWithDifferentObjectTypeCreateDifferentDynamicClasses()
      throws InstantiationException, IllegalAccessException {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    SmartFactoryBeanInterceptor byteBuddyClass =
        (SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, String.class).newInstance();
    final SmartFactoryBean bbAsSmf = (SmartFactoryBean) byteBuddyClass;

    SmartFactoryBeanInterceptor otherByteBuddyClass =
        (SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(FakeObjectConnectionProviderObjectFactory.class, Integer.class).newInstance();
    final SmartFactoryBean ohterBbAsSmf = (SmartFactoryBean) otherByteBuddyClass;

    assertThat(byteBuddyClass.getClass(), is(not(otherByteBuddyClass.getClass())));
    assertThat(bbAsSmf.getObjectType(), is(String.class));
    assertThat(ohterBbAsSmf.getObjectType(), is(Integer.class));
    assertThat(byteBuddyClass.getClass().getSuperclass().getName(), is(otherByteBuddyClass.getClass().getSuperclass().getName()));
  }

  @Test
  public void testGetObjectTypeReturnsSuperIfImplementsObjectTypeProvider()
      throws InstantiationException, IllegalAccessException {
    ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

    SmartFactoryBeanInterceptor byteBuddyClass =
        (SmartFactoryBeanInterceptor) objectFactoryClassRepository
            .getObjectFactoryClass(OtherFakeObjectConnectionProviderObjectFactory.class, String.class).newInstance();
    final SmartFactoryBean bbAsSmf = (SmartFactoryBean) byteBuddyClass;

    assertThat(bbAsSmf.getObjectType(), is(Long.class));
  }

  public static class FakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory {

    @Override
    public Object doGetObject() {
      return new FakeObject();
    }

  }

  public static class OtherFakeObjectConnectionProviderObjectFactory extends AbstractComponentFactory
      implements ObjectTypeProvider {

    @Override
    public Object doGetObject() {
      return new FakeObject();
    }

    @Override
    public Class<?> getObjectType() {
      return Long.class;
    }
  }

  public static class FakeObject extends AbstractComponent {
  }

}
