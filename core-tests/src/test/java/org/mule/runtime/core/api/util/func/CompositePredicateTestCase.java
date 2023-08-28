/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util.func;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.util.func.CompositePredicate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.function.Predicate;

import org.junit.Test;
import org.mockito.InOrder;

@SmallTest
public class CompositePredicateTestCase extends AbstractMuleTestCase {

  private Predicate<String> predicate;

  @Test
  public void nullPredicates() {
    predicate = CompositePredicate.of(null);
    assertThat(predicate.test(EMPTY), is(true));
  }

  @Test
  public void emptyPredicates() {
    predicate = CompositePredicate.of();
    assertThat(predicate.test(EMPTY), is(true));
  }

  @Test
  public void evaluateInOrder() {
    Predicate<String> predicate1 = createPredicate(true);
    Predicate<String> predicate2 = createPredicate(true);

    predicate = CompositePredicate.of(predicate1, predicate2);
    assertThat(predicate.test(EMPTY), is(true));
    InOrder inOrder = inOrder(predicate1, predicate2);
    inOrder.verify(predicate1).test(EMPTY);
    inOrder.verify(predicate2).test(EMPTY);
  }

  @Test
  public void failFast() {
    Predicate<String> predicate1 = createPredicate(false);
    Predicate<String> predicate2 = createPredicate(true);

    predicate = CompositePredicate.of(predicate1, predicate2);
    assertThat(predicate.test(""), is(false));
    InOrder inOrder = inOrder(predicate1, predicate2);
    inOrder.verify(predicate1).test(EMPTY);
    inOrder.verify(predicate2, never()).test(EMPTY);
  }

  private Predicate<String> createPredicate(boolean result) {
    Predicate<String> predicate = mock(Predicate.class);
    when(predicate.test(any())).thenReturn(result);

    return predicate;
  }
}
