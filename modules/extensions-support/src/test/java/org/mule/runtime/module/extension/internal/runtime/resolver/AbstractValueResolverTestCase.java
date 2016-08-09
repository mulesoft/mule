/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

abstract class AbstractValueResolverTestCase extends AbstractMuleContextTestCase {

  protected void assertEvaluation(Object evaluated, Object expected) {
    if (expected == null) {
      assertThat(evaluated, is(nullValue()));
    } else {
      assertThat(evaluated, is(notNullValue()));
      assertThat(evaluated, instanceOf(expected.getClass()));
      assertThat(evaluated, equalTo(expected));
    }
  }

  protected abstract ValueResolver getResolver(String expression);
}
