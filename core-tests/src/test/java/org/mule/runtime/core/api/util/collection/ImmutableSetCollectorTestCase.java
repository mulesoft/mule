/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.runtime.internal.util.collection.ImmutableSetCollector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Set;

import org.junit.Test;

@SmallTest
public class ImmutableSetCollectorTestCase extends AbstractMuleTestCase {

  @Test
  public void collect() {
    Set<String> collected = asList(new String[] {"a", "b", "a", "c"}).stream().collect(new ImmutableSetCollector<>());
    assertThat(collected, hasSize(3));
    assertThat(collected, containsInAnyOrder(new String[] {"a", "b", "c"}));
  }

  @Test
  public void emptySet() {
    Set<String> collected = new ArrayList<String>().stream().collect(new ImmutableSetCollector<>());
    assertThat(collected, is(notNullValue()));
    assertThat(collected, hasSize(0));
  }
}
