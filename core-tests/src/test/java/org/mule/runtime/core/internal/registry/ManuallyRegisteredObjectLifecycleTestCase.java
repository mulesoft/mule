/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ManuallyRegisteredObjectLifecycleTestCase extends AbstractMuleContextTestCase {

  private static final String INITIALISABLE = "initialisable";
  private static final String STARTABLE = "startable";

  public ManuallyRegisteredObjectLifecycleTestCase() {
    setStartContext(true);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> registryObjects = new HashMap<>();
    registryObjects.put("TestInitialisableObject", new TestInitialisableObject());
    registryObjects.put("TestStartableObject", new TestStartableObject());
    return registryObjects;
  }

  @Test
  public void manuallyRegisteredStartableLifecycle() throws Exception {
    assertLifecycle(STARTABLE);
  }

  @Test
  public void manuallyRegisteredInitialisableLifecycle() throws Exception {
    assertLifecycle(INITIALISABLE);
  }


  private void assertLifecycle(String key) {
    TestLifecycleObject testLifecycleObject = ((MuleContextWithRegistries) muleContext).getRegistry().get(key);
    assertThat(testLifecycleObject, is(notNullValue()));

    assertThat(testLifecycleObject.getInitialise(), is(1));
    assertThat(testLifecycleObject.getStart(), is(1));
  }

  private abstract class RegisteringObject implements MuleContextAware {

    private MuleContext muleContext;

    @Override
    public void setMuleContext(MuleContext muleContext) {
      this.muleContext = muleContext;
    }

    protected void manuallyRegisterObject() throws MuleException {
      Object o = new TestLifecycleObject();
      ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(getKey(), o);
    }

    protected abstract String getKey();
  }

  private class TestStartableObject extends RegisteringObject implements Startable {


    @Override
    public void start() throws MuleException {
      manuallyRegisterObject();
    }

    @Override
    protected String getKey() {
      return STARTABLE;
    }
  }

  private class TestInitialisableObject extends RegisteringObject implements Initialisable {

    @Override
    public void initialise() throws InitialisationException {
      try {
        manuallyRegisterObject();
      } catch (MuleException e) {
        throw new InitialisationException(e, this);
      }
    }

    @Override
    protected String getKey() {
      return INITIALISABLE;
    }
  }
}
