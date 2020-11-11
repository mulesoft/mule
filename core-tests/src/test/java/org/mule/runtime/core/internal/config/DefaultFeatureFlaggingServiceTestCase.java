/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.FeatureFlaggingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class DefaultFeatureFlaggingServiceTestCase {

  private static final String FEATURE_ENABLED = "FEATURE A";
  private static final String FEATURE_DISABLED = "FEATURE B";
  private static final String FEATURE_INVALID = "FEATURE INVALID";

  private FeatureFlaggingService featureFlaggingService;

  private final String featureName;
  private final boolean enabled;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    Map<String, Boolean> features = new HashMap<>();
    features.put(FEATURE_ENABLED, true);
    features.put(FEATURE_DISABLED, false);

    featureFlaggingService = new DefaultFeatureFlaggingService(features);
  }

  @Parameters(name = "Feature \"{0}\" should be {1}")
  public static List<Object[]> parameters() {
    return asList(
                  new Object[] {FEATURE_DISABLED, false, null},
                  new Object[] {FEATURE_ENABLED, true, null},
                  new Object[] {FEATURE_INVALID, false, (Consumer<ExpectedException>) (e -> {
                    e.expect(MuleRuntimeException.class);
                    e.expectMessage(format("Feature %s not registered", FEATURE_INVALID));
                  })});

  }

  public DefaultFeatureFlaggingServiceTestCase(String featureName, boolean enabled,
                                               Consumer<ExpectedException> configureExpected) {
    this.featureName = featureName;
    this.enabled = enabled;
    if (configureExpected != null) {
      configureExpected.accept(expectedException);
    }
  }

  @Test
  public void testCase() {
    assertThat(featureFlaggingService.isEnabled(featureName), is(enabled));
  }
}
