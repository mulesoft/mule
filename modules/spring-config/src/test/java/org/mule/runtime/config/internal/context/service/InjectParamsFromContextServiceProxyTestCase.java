/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.service;

import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceProxy.MANY_CANDIDATES_ERROR_MSG_TEMPLATE;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceProxy.NO_OBJECT_FOUND_FOR_PARAM;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.config.utils.Utils.augmentedParam;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.config.utils.Utils.AmbiguousAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedSubclassMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedSubclassOverridesMethodService;
import org.mule.runtime.config.utils.Utils.BaseOverloadedService;
import org.mule.runtime.config.utils.Utils.BaseOverloadedService2;
import org.mule.runtime.config.utils.Utils.BaseService;
import org.mule.runtime.config.utils.Utils.BasicService;
import org.mule.runtime.config.utils.Utils.HiddenAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.InvalidAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.InvalidNamedAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.NamedAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.OverloadedAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.OverloadedAugmentedMethodService2;
import org.mule.runtime.container.internal.MetadataInvocationHandler;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class InjectParamsFromContextServiceProxyTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Inject
  private Registry registry;

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

    expected.expect(IllegalDependencyInjectionException.class);
    expected.expectMessage(format(NO_OBJECT_FOUND_FOR_PARAM, "param", "augmented", "InvalidNamedAugmentedMethodService"));

    serviceProxy.augmented();
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

    expected.expect(IllegalDependencyInjectionException.class);
    expected.expectMessage(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, "augmented", "AmbiguousAugmentedMethodService"));
    serviceProxy.augmented();

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

    final MetadataInvocationHandler noOpHandler = new MetadataInvocationHandler(service) {

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
