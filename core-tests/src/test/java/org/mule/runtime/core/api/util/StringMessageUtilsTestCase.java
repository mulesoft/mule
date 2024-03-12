/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.lang.System.lineSeparator;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class StringMessageUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void toStringOnStringShouldReturnTheString() {
    String input = "Oscar";
    String result = StringMessageUtils.toString(input);
    assertEquals(input, result);
  }

  @Test
  public void toStringOnClassShouldReturnClassName() {
    Object test = getClass();
    String result = StringMessageUtils.toString(test);
    assertEquals(getClass().getName(), result);
  }

  @Test
  public void toStringOnObjectShouldReturnObjectsToStringRepresentation() {
    // this class uses the default toString implementation
    Object test = new TestObject("Ernie");
    String result = StringMessageUtils.toString(test);
    assertEquals(test.toString(), result);

    // this class has a custom toString implementation
    test = new AnotherTestObject("Bert");
    result = StringMessageUtils.toString(test);
    assertEquals("Bert", result);
  }

  @Test
  public void toStringOnStringArrayShouldReturnStringRepresentation() {
    Object test = new String[] {"foo", "bar"};
    String result = StringMessageUtils.toString(test);
    assertEquals("{foo,bar}", result);
  }

  @Test
  public void toStringOnByteArrayShouldReturnStringRepresentation() {
    Object test = new byte[] {1, 2};
    String result = StringMessageUtils.toString(test);
    assertEquals("{1,2}", result);
  }

  @Test
  public void toStringOnByteArrayLargerThanMaximumOutputLengthShouldReturnAbbreviatedStringRepresentation() {
    // create an array that is too long to be printed
    byte[] test = new byte[StringMessageUtils.MAX_ELEMENTS + 100];
    for (int i = 0; i < test.length; i++) {
      test[i] = (byte) i;
    }

    // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
    String result = StringMessageUtils.toString(test);
    assertThat(result, endsWith("[..]}"));
    assertEquals(StringMessageUtils.MAX_ELEMENTS - 1, countMatches(result, ","));
  }

  @Test
  public void toStringOnLongArrayShouldReturnStringRepresentation() {
    long[] test = new long[] {5068875495743534L, 457635546759674L};
    Object result = StringMessageUtils.toString(test);
    assertEquals("{5068875495743534,457635546759674}", result);
  }

  @Test
  public void toStringOnDoubleArrayShouldReturnStringRepresentation() {
    double[] test = new double[] {1.1, 2.02};
    String result = StringMessageUtils.toString(test);
    assertEquals("{1.1,2.02}", result);
  }

  @Test
  public void toStringOnListLargerThanMaximumOutputLengthShouldReturnAbbreviatedStringRepresentation() {
    // create a Collection that is too long to be printed
    List<Integer> list = new ArrayList<>(100);
    for (int i = 0; i < 100; i++) {
      list.add(Integer.valueOf(i));
    }

    // the String will contain not more than exactly MAX_ARRAY_LENGTH elements
    String result = StringMessageUtils.toString(list);
    assertThat(result, endsWith("[..]]"));
    assertEquals(StringMessageUtils.MAX_ELEMENTS - 1, countMatches(result, ","));
  }

  @Test
  public void testBoilerPlateSingleLine() {
    String plate = StringMessageUtils.getBoilerPlate("Single message.", '*', 12);
    assertEquals(lineSeparator() + "************" + lineSeparator() + "* Single   *"
        + lineSeparator() + "* message. *" + lineSeparator() + "************", plate);
  }

  @Test
  public void testBoilerPlate() throws Exception {
    List<String> msgs = new ArrayList<>();
    msgs.add("This");
    msgs.add("is a");
    msgs.add("Boiler Plate");

    String plate = StringMessageUtils.getBoilerPlate(msgs, '*', 12);
    assertEquals(lineSeparator() + "************" + lineSeparator() + "* This     *"
        + lineSeparator() + "* is a     *" + lineSeparator() + "* Boiler   *" + lineSeparator()
        + "* Plate    *" + lineSeparator() + "************", plate);

  }

  @Test
  public void testBoilerPlate2() throws Exception {
    List<String> msgs = new ArrayList<>();
    msgs.add("This");
    msgs.add("is a");
    msgs.add("Boiler Plate Message that should get wrapped to the next line if it is working properly");

    String plate = StringMessageUtils.getBoilerPlate(msgs, '*', 12);
    assertEquals(lineSeparator() + "************" + lineSeparator() + "* This     *"
        + lineSeparator() + "* is a     *" + lineSeparator() + "* Boiler   *" + lineSeparator()
        + "* Plate    *" + lineSeparator() + "* Message  *" + lineSeparator() + "* that     *"
        + lineSeparator() + "* should   *" + lineSeparator() + "* get      *" + lineSeparator()
        + "* wrapped  *" + lineSeparator() + "* to the   *" + lineSeparator() + "* next     *"
        + lineSeparator() + "* line if  *" + lineSeparator() + "* it is    *" + lineSeparator()
        + "* working  *" + lineSeparator() + "* properly *" + lineSeparator() + "************", plate);
  }

  @Test
  public void testTruncate() {
    String msg = "this is a test message for truncating";
    String result = StringMessageUtils.truncate(msg, 100, true);
    assertEquals(msg, result);

    result = StringMessageUtils.truncate(msg, 10, false);
    assertEquals("this is a ...", result);

    result = StringMessageUtils.truncate(msg, 10, true);
    assertEquals("this is a ...[10 of 37]", result);
  }

  private class TestObject {

    private final String myName;

    public TestObject(String name) {
      this.myName = name;
    }

    public String getName() {
      return myName;
    }
  }

  private class AnotherTestObject extends TestObject {

    public AnotherTestObject(String name) {
      super(name);
    }

    @Override
    public String toString() {
      return getName();
    }
  }
}
