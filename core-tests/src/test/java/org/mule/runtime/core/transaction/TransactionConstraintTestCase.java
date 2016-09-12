/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transaction;

import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.runtime.core.transaction.constraints.ConstraintFilter;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class TransactionConstraintTestCase extends AbstractMuleTestCase {

  @Test
  public void testConstraintFilter() throws Exception {
    ConstraintFilter filter = new ConstraintFilter();
    Event event = Mockito.mock(Event.class);
    assertTrue(filter.accept(event));

    ConstraintFilter clone = (ConstraintFilter) filter.clone();
    assertNotNull(clone);
    assertNotSame(filter, clone);
  }
}
