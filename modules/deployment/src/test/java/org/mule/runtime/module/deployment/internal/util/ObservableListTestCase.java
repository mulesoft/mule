/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.util;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Collections;
import java.util.ListIterator;

public class ObservableListTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  // Must be equal to java.util.Collections.REVERSE_THRESHOLD
  // Collections#reverse() uses two different algorithms.
  private static final int COLLECTIONS_REVERSE_THRESHOLD = 18;

  @Test
  public void testListIteratorRemove() {
    ObservableList<String> list = new ObservableList<String>();
    list.add("a");
    list.add("b");
    ListIterator<String> it = list.listIterator();
    it.next();
    it.remove();

    assertThat(it.hasNext(), is(true));
    assertThat(it.nextIndex(), equalTo(0));
  }

  @Test
  public void testListIteratorRemoveWithoutNext() {
    thrown.expect(IllegalStateException.class);

    ObservableList<String> list = new ObservableList<String>();
    list.add("a");
    ListIterator<String> it = list.listIterator();
    it.remove();
  }

  @Test
  public void testListIteratorSet() {
    ObservableList<String> list = new ObservableList<String>();
    list.add("a");
    ListIterator<String> it = list.listIterator();
    String s = it.next();
    it.set("A");

    assertThat(s, equalTo("a"));
    assertThat(list.get(0), equalTo("A"));
  }

  @Test
  public void testListIteratorSetWithoutNext() {
    thrown.expect(IllegalStateException.class);

    ObservableList<String> list = new ObservableList<String>();
    list.add("a");
    ListIterator<String> it = list.listIterator();
    it.set("A");
  }

  @Test
  public void testReverseObservableListWith18Elements() {
    ObservableList<Integer> list = createObservableList(COLLECTIONS_REVERSE_THRESHOLD);
    Collections.reverse(list);

    assertThat(list.get(0), equalTo(COLLECTIONS_REVERSE_THRESHOLD - 1));
    assertThat(list.get(COLLECTIONS_REVERSE_THRESHOLD - 1), equalTo(0));
  }

  @Test
  public void testReverseObservableListWith17Elements() {
    ObservableList<Integer> list = createObservableList(COLLECTIONS_REVERSE_THRESHOLD - 1);
    Collections.reverse(list);

    assertThat(list.get(0), equalTo(COLLECTIONS_REVERSE_THRESHOLD - 2));
    assertThat(list.get(COLLECTIONS_REVERSE_THRESHOLD - 2), equalTo(0));
  }

  private ObservableList<Integer> createObservableList(int n) {
    ObservableList<Integer> list = new ObservableList<Integer>();
    for (int i = 0; i < n; i++) {
      list.add(i);
    }
    return list;
  }
}
