/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime;

import static java.util.ResourceBundle.Control.FORMAT_DEFAULT;
import static java.util.ResourceBundle.Control.getControl;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import java.util.ResourceBundle.Control;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@OutputTimeUnit(MILLISECONDS)
@State(Benchmark)
public class I18nMessageFactoryBenchmark {

  public static final String TEST_MESSAGE_PARAMETER = "Testing!";

  @Param({"true", "false"})
  public boolean customControl;

  @Setup
  public void setUp() {
    TestI18nMessages.customControl = customControl;
    TestI18nMessages.testMessage(TEST_MESSAGE_PARAMETER);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public I18nMessage createI18nMessage() {
    return TestI18nMessages.testMessage(TEST_MESSAGE_PARAMETER);
  }

  public static class TestI18nMessages extends I18nMessageFactory {

    private static final TestI18nMessages factory = new TestI18nMessages();

    private static final Control defaultControl = getControl(FORMAT_DEFAULT);

    public static boolean customControl = true;

    private static final String BUNDLE_PATH = getBundlePath("test");

    public static I18nMessage testMessage(String arg) {
      return factory.createMessage(BUNDLE_PATH, 1, arg);
    }

    @Override
    protected Control getReloadControl() {
      if (customControl) {
        return super.getReloadControl();
      } else {
        return defaultControl;
      }
    }
  }

}
