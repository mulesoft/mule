/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.service;

import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceMethodInvoker.MANY_CANDIDATES_ERROR_MSG_TEMPLATE;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceMethodInvoker.NO_OBJECT_FOUND_FOR_PARAM;
import static org.mule.runtime.config.utils.Utils.augmentedParam;

import static java.lang.String.format;
import static java.util.Arrays.asList;

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
import org.mule.runtime.config.utils.UtilsJavax.BaseService;
import org.mule.runtime.config.utils.UtilsJavax.BasicService;
import org.mule.runtime.config.utils.UtilsJavax.HiddenAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.InvalidAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.InvalidNamedAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.NamedAugmentedMethodService;
import org.mule.runtime.config.utils.UtilsJavax.OverloadedAugmentedMethodService;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

public class InjectParamsFromContextServiceMethodInvokerJavaxTestCase extends AbstractMuleContextTestCase {

  @Inject
  private Registry registry;

  private InjectParamsFromContextServiceMethodInvoker injectParamsFromContextServiceMethodInvoker;
  private Method method;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Before
  public void setUp() throws NoSuchMethodException {
    injectParamsFromContextServiceMethodInvoker = new InjectParamsFromContextServiceMethodInvoker(registry);
    method = BaseService.class.getMethod("augmented");
  }

  @Test
  public void notAugmentedInvocation() throws Throwable {
    BaseService service = new BasicService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void augmentedInvocation() throws Throwable {
    BaseService service = new AugmentedMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void augmentedSubclassInvocation() throws Throwable {
    BaseService service = new AugmentedSubclassMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void augmentedSubclassOverridesInvocation() throws Throwable {
    BaseService service = new AugmentedSubclassOverridesMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void namedAugmentedInvocation() throws Throwable {
    BaseService service = new NamedAugmentedMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void invalidNamedAugmentedInvocation() throws Throwable {
    BaseService service = new InvalidNamedAugmentedMethodService();

    var thrown = assertThrows(IllegalDependencyInjectionException.class,
                              () -> injectParamsFromContextServiceMethodInvoker.invoke(service, method, null));
    assertThat(thrown.getMessage(), is(format(NO_OBJECT_FOUND_FOR_PARAM, "param", method.getName(), service.toString())));
  }

  @Test
  public void hiddenAugmentedInvocation() throws Throwable {
    BaseService service = new HiddenAugmentedMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void overloadedAugmentedInvocation() throws Throwable {
    BaseOverloadedService service = new OverloadedAugmentedMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void overloadedAugmentedInvocation2() throws Throwable {
    BaseOverloadedService service = new OverloadedAugmentedMethodService();

    java.util.List<Method> methods = asList(OverloadedAugmentedMethodService.class.getMethods());

    Optional<Method> method = methods.stream().filter(m -> m.getName().equals("augmented") && m.getParameterCount() == 1
        && !m.getParameters()[0].getName().contains("context")).findFirst();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method.get(), new Object[] {1});

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void ambiguousAugmentedInvocation() throws Throwable {
    BaseService service = new AmbiguousAugmentedMethodService();

    var thrown = assertThrows(IllegalDependencyInjectionException.class,
                              () -> injectParamsFromContextServiceMethodInvoker.invoke(service, method, null));
    assertThat(thrown.getMessage(), is(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, method.getName(), service.toString())));

    assertThat(augmentedParam, nullValue());
  }

  @Test
  public void invalidAugmentedInvocation() throws Throwable {
    BaseService service = new InvalidAugmentedMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, is(true));
  }

}
