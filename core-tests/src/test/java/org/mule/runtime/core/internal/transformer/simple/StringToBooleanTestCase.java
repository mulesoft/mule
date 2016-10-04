/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.stream.Stream;

import org.junit.Test;

@SmallTest
public class StringToBooleanTestCase extends AbstractMuleTestCase {

  private StringToBoolean transformer = new StringToBoolean();

  @Test
  public void validTransforms() {
    assertBoolean(true, "true", "TRUE", "trUE ", "yes", "1");
    assertBoolean(false, "false", "FALSE", "no ", "NO ", "0");
  }

  @Test(expected = TransformerException.class)
  public void invalidValue() throws Exception {
    transformer.transform("cuchurrumin");
  }

  private void assertBoolean(boolean expected, String... values) {
    Stream.of(values).forEach(value -> {
      try {
        assertThat(transformer.transform(value), is(expected));
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
    });
  }
}
