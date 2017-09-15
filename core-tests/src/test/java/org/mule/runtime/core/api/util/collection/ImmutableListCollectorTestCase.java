/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableList;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ImmutableListCollectorTestCase extends AbstractMuleTestCase {

  private final String[] items = new String[] {"a", "b", "c"};

  @Test
  public void collect() {
    List<String> collected = Arrays.asList(items).stream().collect(toImmutableList());
    assertThat(collected, hasSize(items.length));
    assertThat(collected, contains(items));
  }

  @Test
  public void emptyList() {
    List<String> collected = new ArrayList<String>().stream().collect(toImmutableList());
    assertThat(collected, is(notNullValue()));
    assertThat(collected, hasSize(0));
  }
}
