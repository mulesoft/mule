/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.config.utils.Utils.AmbiguousAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedSubclassMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedSubclassOverridesMethodService;
import org.mule.runtime.config.utils.Utils.AugmentedWithPreferredMethodService;
import org.mule.runtime.config.utils.Utils.BaseOverloadedService;
import org.mule.runtime.config.utils.Utils.BaseService;
import org.mule.runtime.config.utils.Utils.BasicService;
import org.mule.runtime.config.utils.Utils.HiddenAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.InvalidAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.InvalidNamedAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.MyBean;
import org.mule.runtime.config.utils.Utils.MyPreferredBean;
import org.mule.runtime.config.utils.Utils.NamedAugmentedMethodService;
import org.mule.runtime.config.utils.Utils.OverloadedAugmentedMethodService;
import org.mule.runtime.config.internal.InjectParamsFromContextServiceMethodInvoker;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceMethodInvoker.MANY_CANDIDATES_ERROR_MSG_TEMPLATE;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceMethodInvoker.NO_OBJECT_FOUND_FOR_PARAM;
import static org.mule.runtime.config.utils.Utils.augmentedParam;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

public class InjectParamsFromContextServiceMethodInvokerTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

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
  public void augmentedWithPreferredInvocation() throws Throwable {
    registerObject(muleContext, "myBean", new MyBean());
    final MyPreferredBean preferredBean = new MyPreferredBean();
    registerObject(muleContext, "myPreferredBean", preferredBean);

    BaseService service = new AugmentedWithPreferredMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, sameInstance(preferredBean));
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

    expected.expect(IllegalDependencyInjectionException.class);
    expected.expectMessage(format(NO_OBJECT_FOUND_FOR_PARAM, "param", method.getName(), service.toString()));

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);
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

    expected.expectMessage(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, method.getName(), service.toString()));
    expected.expect(IllegalDependencyInjectionException.class);
    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, nullValue());
  }

  @Test
  public void invalidAugmentedInvocation() throws Throwable {
    BaseService service = new InvalidAugmentedMethodService();

    injectParamsFromContextServiceMethodInvoker.invoke(service, method, null);

    assertThat(augmentedParam, is(true));
  }

}
