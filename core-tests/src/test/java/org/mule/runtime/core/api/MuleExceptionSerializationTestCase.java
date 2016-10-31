/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.exception.MuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ObjectStreamClass;

import org.junit.Test;

@SmallTest
public class MuleExceptionSerializationTestCase extends AbstractMuleTestCase {

  @Test
  public void hasCorrectSerialVersionUID() {
    assertThat(ObjectStreamClass.lookup(MuleException.class).getSerialVersionUID(), is(-4544199933449632546L));
  }

}
