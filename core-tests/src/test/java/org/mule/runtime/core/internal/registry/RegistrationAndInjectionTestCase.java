/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;

import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.lifecycle.LifecycleStateAware;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;

public class RegistrationAndInjectionTestCase extends AbstractMuleContextTestCase {

  private static final String KEY = "key";
  private static final String KEY2 = "key2";
  private static final String EXTENDED_KEY = "extendedKey";

  public RegistrationAndInjectionTestCase() {
    setStartContext(true);
  }

  @Test
  public void applyLifecycleUponRegistration() throws Exception {
    TestLifecycleObject object = registerObject();
    assertRegistered(object);
    assertInitialisation(object);
  }

  @Test
  public void applyLifecycleUponUnregistration() throws Exception {
    applyLifecycleUponRegistration();
    TestLifecycleObject object =
        (TestLifecycleObject) ((MuleContextWithRegistries) muleContext).getRegistry().unregisterObject(KEY);
    assertThat(object, is(notNullValue()));
    assertShutdown(object);
  }

  @Test
  public void registerExistingKey() throws Exception {
    TestLifecycleObject object = registerObject();
    TestLifecycleObject replacement = registerObject();

    assertRegistered(replacement);
    assertShutdown(object);
    assertInitialisation(replacement);
  }

  @Test
  public void injectOnRegisteredObject() throws Exception {
    TestLifecycleObject object = registerObject();
    assertInjection(object);
  }

  @Test
  public void injectWithInheritance() throws Exception {
    TestLifecycleObject child1 = new TestLifecycleObject();
    TestLifecycleObject child2 = new TestLifecycleObject();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(KEY, child1);
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(KEY2, child2);

    assertThat(((MuleContextWithRegistries) muleContext).getRegistry().lookupByType(TestLifecycleObject.class).size(), is(2));

    ExtendedTestLifecycleObject object = new ExtendedTestLifecycleObject();

    muleContext.getInjector().inject(object);
    // muleContext.getRegistry().registerObject(EXTENDED_KEY, object);
    assertInjection(object);
    assertThat(object.getKeyChild(), is(sameInstance(child1)));
    assertThat(object.getKey2Child(), is(sameInstance(child2)));
  }

  @Test
  public void muleContextAware() throws Exception {
    MuleContextAware muleContextAware = mock(MuleContextAware.class);
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(KEY, muleContextAware);
    assertRegistered(muleContextAware);
    verify(muleContextAware).setMuleContext(muleContext);
  }

  @Test
  public void lifecycleSateAware() throws Exception {
    LifecycleStateAware lifecycleStateAware = mock(LifecycleStateAware.class);
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(KEY, lifecycleStateAware);
    assertRegistered(lifecycleStateAware);
    verify(lifecycleStateAware).setLifecycleState(any(LifecycleState.class));
  }

  private void assertInjection(TestLifecycleObject object) {
    assertThat(((MuleContextWithRegistries) muleContext).getRegistry().get(OBJECT_STORE_MANAGER),
               is(object.getObjectStoreManager()));
    assertThat(object.getMuleContext(), is(muleContext));

    // just to make sure that injection is to thank for this
    assertThat(object, is(not(instanceOf(MuleContextAware.class))));
  }

  private void assertRegistered(Object object) {
    Object registered = ((MuleContextWithRegistries) muleContext).getRegistry().get(KEY);
    assertThat(registered, is(sameInstance(object)));
  }

  private TestLifecycleObject registerObject() throws RegistrationException {
    TestLifecycleObject object = new TestLifecycleObject();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(KEY, object);

    return object;
  }

  private void assertInitialisation(TestLifecycleObject object) {
    assertThat(object.getInitialise(), is(1));
    assertThat(object.getStart(), is(1));
    assertThat(object.getStop(), is(0));
    assertThat(object.getDispose(), is(0));
  }

  private void assertShutdown(TestLifecycleObject object) {
    assertThat(object.getStop(), is(1));
    assertThat(object.getDispose(), is(1));
  }

  public static class ExtendedTestLifecycleObject extends TestLifecycleObject {

    @Inject
    @Named(KEY)
    private TestLifecycleObject keyChild;

    private TestLifecycleObject key2Child;

    public TestLifecycleObject getKeyChild() {
      return keyChild;
    }

    public TestLifecycleObject getKey2Child() {
      return key2Child;
    }

    @Inject
    @Named(KEY2)
    public void setKey2Child(TestLifecycleObject key2Child) {
      this.key2Child = key2Child;
    }
  }
}
