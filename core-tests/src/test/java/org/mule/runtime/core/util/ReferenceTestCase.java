/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.util.Reference;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ReferenceTestCase extends AbstractMuleTestCase {

  @Test
  public void get() {
    final Object value = new Object();
    Reference<Object> ref = new Reference<>(value);
    assertThat(ref.get(), is(sameInstance(value)));

  }

  @Test
  public void valueHashCode() {
    final Object value = new Object();
    Reference<Object> ref = new Reference<>(value);
    assertThat(ref.hashCode(), is(value.hashCode()));
  }

  @Test
  public void getFromNullReference() {
    assertThat(new Reference<>(null).get(), is(nullValue()));
  }

  @Test
  public void nullValueHashCode() {
    assertThat(new Reference<>(null).hashCode(), is(0));
  }

  @Test
  public void equalReferences() {
    final Object value = new Object();
    assertEquals(new Reference<>(value), new Reference<>(value));
  }

  @Test
  public void equalNullReferences() {
    assertEquals(new Reference<>(null), new Reference<>(null));
  }

  @Test
  public void equalMixedReferences() {
    assertNotEquals(new Reference<>(null), new Reference<>(new Object()));
    assertNotEquals(new Reference<>(new Object()), new Reference<>(null));
  }

  @Test
  public void notEqualReferences() {
    assertNotEquals(new Reference<>(new Object()), new Reference<>(new Object()));
  }

  private <T> void assertEquals(Reference<T> ref1, Reference<T> ref2) {
    assertThat(ref1, equalTo(ref2));
    assertThat(ref1.hashCode(), is(ref2.hashCode()));
  }

  private <T> void assertNotEquals(Reference<T> ref1, Reference<T> ref2) {
    assertThat(ref1, not(equalTo(ref2)));
    assertThat(ref1.hashCode(), not(equalTo(ref2.hashCode())));
  }
}
