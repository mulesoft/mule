/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.service;

import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceUtils.MANY_CANDIDATES_ERROR_MSG_TEMPLATE;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceUtils.NO_OBJECT_FOUND_FOR_PARAM;
import static org.mule.runtime.config.utils.UtilsJavax.augmentedParam;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.config.utils.UtilsJavax.AmbiguousAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.AugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.AugmentedSubclassMethodService;
import org.mule.runtime.config.utils.UtilsJavax.AugmentedSubclassOverridesMethodService;
import org.mule.runtime.config.utils.UtilsJavax.BaseOverloadedService;
import org.mule.runtime.config.utils.UtilsJavax.BaseOverloadedService2;
import org.mule.runtime.config.utils.UtilsJavax.BaseService;
import org.mule.runtime.config.utils.UtilsJavax.BasicService;
import org.mule.runtime.config.utils.UtilsJavax.HiddenAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.InvalidAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.InvalidNamedAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.NamedAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.OverloadedAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.OverloadedAugmentedMethodService2;
import org.mule.runtime.container.internal.MetadataInvocationHandler;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

@SmallTest
public class InjectParamsFromContextServiceProxyJavaxTestCase extends AbstractMuleContextTestCase {

  @Inject
  private Registry registry;

  @Before
  public void setUo() {
    augmentedParam = null;
  }

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Test
  public void notAugmentedInvocation() {
    BaseService service = new BasicService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void augmentedInvocation() {
    BaseService service = new AugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void augmentedSubclassInvocation() {
    BaseService service = new AugmentedSubclassMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void augmentedSubclassOverridesInvocation() {
    BaseService service = new AugmentedSubclassOverridesMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void namedAugmentedInvocation() {
    BaseService service = new NamedAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void invalidNamedAugmentedInvocation() {
    BaseService service = new InvalidNamedAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    var thrown = assertThrows(IllegalDependencyInjectionException.class,
                              () -> serviceProxy.augmented());
    assertThat(thrown.getMessage(),
               is(format(NO_OBJECT_FOUND_FOR_PARAM, "param", "augmented", "InvalidNamedAugmentedMethodService")));
  }

  @Test
  public void hiddenAugmentedInvocation() {
    BaseService service = new HiddenAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void overloadedAugmentedInvocation() {
    BaseOverloadedService service = new OverloadedAugmentedMethodService();

    final BaseOverloadedService serviceProxy =
        (BaseOverloadedService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void overloadedAugmentedInvocation2() {
    BaseOverloadedService service = new OverloadedAugmentedMethodService();

    final BaseOverloadedService serviceProxy =
        (BaseOverloadedService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented(1);

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void overloadedAugmentedInvocation3() {
    BaseOverloadedService2 service = new OverloadedAugmentedMethodService2();

    final BaseOverloadedService2 serviceProxy =
        (BaseOverloadedService2) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented(muleContext, 1);

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void ambiguousAugmentedInvocation() {
    BaseService service = new AmbiguousAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    var thrown = assertThrows(IllegalDependencyInjectionException.class,
                              () -> serviceProxy.augmented());
    assertThat(thrown.getMessage(),
               is(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, "augmented", "AmbiguousAugmentedMethodService")));

    assertThat(augmentedParam, nullValue());
  }

  @Test
  public void invalidAugmentedInvocation() {
    BaseService service = new InvalidAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void throughProxyAugmentedInvocation() {
    BaseService service = new AugmentedMethodService();

    final MetadataInvocationHandler<BaseService> noOpHandler = new MetadataInvocationHandler<>(service) {

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(service, args);
      }
    };
    final BaseService innerProxy =
        (BaseService) newProxyInstance(service.getClass().getClassLoader(), new Class<?>[] {BaseService.class}, noOpHandler);
    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(innerProxy, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

}
