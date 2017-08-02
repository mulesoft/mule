/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class GetArrayLengthTestCase extends AbstractMuleTestCase {

  private final Object unaryArray;
  private final Object emptyArray;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{new Object[] {new Apple()}, new Object[] {}},
        {new Apple[] {new Apple()}, new Apple[] {}}, {new boolean[] {true}, new boolean[] {}}, {new byte[] {0}, new byte[] {}},
        {new char[] {'0'}, new char[] {}}, {new short[] {0}, new short[] {}}, {new int[] {0}, new int[] {}},
        {new long[] {0}, new long[] {}}, {new float[] {0}, new float[] {}}, {new double[] {0}, new double[] {}},
        {new String[] {""}, new String[] {}}, {new String[] {""}, null},});
  }

  public GetArrayLengthTestCase(Object unaryArray, Object emptyArray) {
    this.unaryArray = unaryArray;
    this.emptyArray = emptyArray;
  }


  @Test
  public void unaryArray() {
    assertThat(ArrayUtils.getLength(unaryArray), is(1));
  }

  @Test
  public void emptyArray() {
    assertThat(ArrayUtils.getLength(emptyArray), is(0));
  }
}
