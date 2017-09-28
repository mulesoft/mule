/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceProxy.MANY_CANDIDATES_ERROR_MSG_TEMPLATE;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceProxy.NO_OBJECT_FOUND_FOR_PARAM;
import static org.mule.runtime.config.internal.InjectParamsFromContextServiceProxy.createInjectProviderParamsServiceProxy;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.container.api.MetadataInvocationHandler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.runtime.core.internal.config.preferred.Preferred;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;

@SmallTest
public class InjectParamsFromContextServiceProxyTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Inject
  private Registry registry;

  private Object augmentedParam;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

  @Test
  public void notAugmentedInvocation() throws Exception {
    BaseService service = new BasicService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void augmentedInvocation() throws Exception {
    BaseService service = new AugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void augmentedSubclassInvocation() throws Exception {
    BaseService service = new AugmentedSubclassMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void augmentedSubclassOverridesInvocation() throws Exception {
    BaseService service = new AugmentedSubclassOverridesMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void augmentedWithPreferredInvocation() throws Exception {
    registerObject(muleContext, "myBean", new MyBean());
    final MyPreferredBean preferredBean = new MyPreferredBean();
    registerObject(muleContext, "myPreferredBean", preferredBean);

    BaseService service = new AugmentedWithPreferredMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(preferredBean));
  }

  @Test
  public void namedAugmentedInvocation() throws Exception {
    BaseService service = new NamedAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void invalidNamedAugmentedInvocation() throws Exception {
    BaseService service = new InvalidNamedAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    expected.expect(IllegalDependencyInjectionException.class);
    expected.expectMessage(format(NO_OBJECT_FOUND_FOR_PARAM, "param", "augmented", "InvalidNamedAugmentedMethodService"));

    serviceProxy.augmented();
  }

  @Test
  public void hiddenAugmentedInvocation() throws Exception {
    BaseService service = new HiddenAugmentedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void overloadedAugmentedInvocation() throws Exception {
    BaseOverloadedService service = new OverloadedAugmentedMethodService();

    final BaseOverloadedService serviceProxy =
        (BaseOverloadedService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void overloadedAugmentedInvocation2() throws Exception {
    BaseOverloadedService service = new OverloadedAugmentedMethodService();

    final BaseOverloadedService serviceProxy =
        (BaseOverloadedService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented(1);

    assertThat(augmentedParam, sameInstance(muleContext));
  }

  @Test
  public void ambiguousAugmentedInvocation() throws Exception {
    BaseService service = new AmbiguousAugmetedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    expected.expect(IllegalDependencyInjectionException.class);
    expected.expectMessage(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, "augmented", "AmbiguousAugmentedMethodService"));
    serviceProxy.augmented();

    assertThat(augmentedParam, nullValue());
  }

  @Test
  public void invalidAugmentedInvocation() throws Exception {
    BaseService service = new InvalidAugmetedMethodService();

    final BaseService serviceProxy = (BaseService) createInjectProviderParamsServiceProxy(service, registry);

    serviceProxy.augmented();

    assertThat(augmentedParam, is(true));
  }

  @Test
  public void throughProxyAugmentedInvocation() throws Exception {
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

  public interface BaseService extends Service {

    void augmented();
  }

  public interface BaseOverloadedService extends BaseService {

    void augmented(int i);
  }

  public class BasicService implements BaseService {

    @Override
    public String getName() {
      return "BasicService";
    }

    @Override
    public void augmented() {
      augmentedParam = true;
    }

  }

  public class AugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "AugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = context;
    }
  }

  public class AugmentedSubclassMethodService extends AugmentedMethodService {

    @Override
    public String getName() {
      return "AugmentedSubclassMethodService";
    }

  }

  public class AugmentedSubclassOverridesMethodService extends AugmentedMethodService {

    @Override
    public String getName() {
      return "AugmentedSubclassOverridesMethodService";
    }

    @Override
    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = true;
    }
  }

  public class AugmentedWithPreferredMethodService implements BaseService {

    @Override
    public String getName() {
      return "AugmentedWithPreferredMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(MyBean context) {
      augmentedParam = context;
    }
  }

  public class MyBean {

  }

  @Preferred
  public class MyPreferredBean extends MyBean {

  }

  public class NamedAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "NamedAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(@Named(OBJECT_MULE_CONTEXT) Object param) {
      augmentedParam = param;
    }
  }

  public class InvalidNamedAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "InvalidNamedAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(@Named("!@#$%&*_" + OBJECT_MULE_CONTEXT) Object param) {
      augmentedParam = param;
    }
  }

  public class HiddenAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "HiddenAugmentedMethodService";
    }

    @Override
    public void augmented() {
      augmentedParam = true;
    }

    @Inject
    private void augmented(MuleContext context) {
      augmentedParam = context;
    }
  }

  public class OverloadedAugmentedMethodService implements BaseOverloadedService {

    @Override
    public String getName() {
      return "OverloadedAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Override
    public void augmented(int i) {}

    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = true;
    }

    @Inject
    public void augmented(int i, MuleContext context) {
      augmentedParam = context;
    }
  }

  public class AmbiguousAugmetedMethodService implements BaseService {

    @Override
    public String getName() {
      return "AmbiguousAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = context;
    }

    @Inject
    public void augmented(MuleContext context, MuleContext contextB) {
      augmentedParam = context;
    }
  }

  public class InvalidAugmetedMethodService implements BaseService {

    @Override
    public String getName() {
      return "InvalidAugmetedMethodService";
    }

    @Override
    public void augmented() {
      augmentedParam = true;
    }

    @Inject
    void augmented(int i) {}
  }


}
