/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.store;

import static org.hamcrest.Matchers.is;
import static org.mule.runtime.core.privileged.store.DeserializationPostInitialisable.Implementation.init;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DeserializationPostInitialisableTestCase extends AbstractMuleContextTestCase {

  @Test
  public void initsObjectWhichImplementsMethod() throws Exception {
    ClassWithMethod objectWhichImplementsMethod = new ClassWithMethod();
    init(objectWhichImplementsMethod, muleContext);
    assertThat(objectWhichImplementsMethod.isInitialized(), is(true));
  }

  @Test
  public void initsObjectWhichInheritsMethod() throws Exception {
    ClassWithMethod objectWhichInheritsMethod = new ChildClass();
    init(objectWhichInheritsMethod, muleContext);
    assertThat(objectWhichInheritsMethod.isInitialized(), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void tryToInitObjectWithoutMethodThrowsException() throws Exception {
    Object objectWhichDoesntImplementMethod = new Object();
    init(objectWhichDoesntImplementMethod, muleContext);
  }

  private static class ClassWithMethod implements DeserializationPostInitialisable {

    boolean initialized = false;

    @SuppressWarnings({"unused"})
    private void initAfterDeserialisation(MuleContext muleContext) {
      initialized = true;
    }

    boolean isInitialized() {
      return initialized;
    }
  }

  private static class ChildClass extends ClassWithMethod {

  }
}
