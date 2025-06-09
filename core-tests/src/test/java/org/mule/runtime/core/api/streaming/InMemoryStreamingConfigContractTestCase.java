/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;

public abstract class InMemoryStreamingConfigContractTestCase extends AbstractMuleTestCase {

  @Test
  @Description("Test that a valid config is indeed valid")
  void initialSizeEqualsMaxSizeWithNoExpansion() {
    assertValid(1, 0, 1);
  }

  @Test
  @Description("initial size of zero should be invalid")
  void zeroInitialSize() {
    assertInvalid(0, 1, 10);
  }

  @Test
  @Description("initial size cannot be negative")
  void negativeInitialSIze() {
    assertInvalid(-1, 1, 10);
  }

  @Test
  @Description("increment size cannot be negative")
  void negativeIncrementSize() {
    assertInvalid(10, -1, 20);
  }

  @Test
  @Description("initial size cannot be bigger the maxSize")
  void initialSizeBiggerThanMaxSize() {
    assertInvalid(100, 1, 10);
  }

  @Test
  @Description("incrementSize cannot be bigger than the maxSize")
  void incrementSizeBiggerThanMaxSize() {
    assertInvalid(10, 100, 20);
  }

  @Test
  @Description("initialSize + incrementSize cannot be bigger than maxSize")
  void invalidExpansionSize() {
    assertInvalid(1, 100, 100);
  }

  private void assertValid(int initialSize, int increment, int maxSize) {
    createConfig(initialSize, increment, maxSize);
  }

  private void assertInvalid(int initialSize, int increment, int maxSize) {
    assertThrows(IllegalArgumentException.class, () -> createConfig(initialSize, increment, maxSize));
  }

  protected abstract void createConfig(int initialSize, int increment, int maxSize);
}
