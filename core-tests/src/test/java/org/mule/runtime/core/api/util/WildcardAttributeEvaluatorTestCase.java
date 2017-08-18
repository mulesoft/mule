/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class WildcardAttributeEvaluatorTestCase extends AbstractMuleTestCase {

  @Test
  public void testStartsWithWildcard() {
    List<String> testValues = Arrays.asList("MULE", "MULEMAN", "EMULE", "MULE\\*", "\\*MULE");
    List<String> expectedValues = Arrays.asList("MULE", "MULEMAN", "MULE\\*");
    testScenario("MULE*", testValues, expectedValues);
  }

  @Test
  public void testEndsWithWildcard() {
    List<String> testValues = Arrays.asList("MULE", "EMULE", "MAN-MULE-MAN", "\\*MULE", "MULE\\*");
    List<String> expectedValues = Arrays.asList("MULE", "EMULE", "\\*MULE");
    testScenario("*MULE", testValues, expectedValues);
  }

  @Test
  public void testAllWildcard() {
    List<String> testValues = Arrays.asList("MULE", "EMULE", "MAN-MULE-MAN", "\\*MULE", "MULE\\*");
    List<String> expectedValues = Arrays.asList("MULE", "EMULE", "MAN-MULE-MAN", "\\*MULE", "MULE\\*");
    testScenario("*", testValues, expectedValues);
  }

  @Test
  public void testWithEscapedCharactersOnly() {
    WildcardAttributeEvaluator wildcardAttributeEvaluator = new WildcardAttributeEvaluator("\\*");
    Assert.assertThat(wildcardAttributeEvaluator.hasWildcards(), Is.is(false));
  }

  @Test
  public void testWithEscapedCharactersAndWildcards() {
    List<String> testValues = Arrays.asList("\\*MULE", "EMULE", "MAN-MULE-MAN", "", "MULE\\*", "\\*MULE\\*");
    List<String> expectedValues = Arrays.asList("\\*MULE", "\\*MULE\\*");
    testScenario("\\*MULE*", testValues, expectedValues);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCallConstructorWithNull() {
    new WildcardAttributeEvaluator(null);
  }

  private void testScenario(String attributeValue, final List<String> testValues, final List<String> expectedValues) {
    WildcardAttributeEvaluator wildcardAttributeEvaluator = new WildcardAttributeEvaluator(attributeValue);
    assertThat(wildcardAttributeEvaluator.hasWildcards(), Is.is(true));
    final List<String> resultingValues = new ArrayList<String>();
    wildcardAttributeEvaluator.processValues(testValues, new WildcardAttributeEvaluator.MatchCallback() {

      @Override
      public void processMatch(String matchedValue) {
        resultingValues.add(matchedValue);
      }
    });
    assertThat(expectedValues.size(), Is.is(resultingValues.size()));
    for (String expectedValue : expectedValues) {
      assertThat(resultingValues.contains(expectedValue), Is.is(true));
    }
  }
}
