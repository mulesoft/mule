/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.util.MuleSystemProperties.FOREACH_ROUTER_REJECTS_MAP_EXPRESSIONS_PROPERTY;
import static org.mule.runtime.core.internal.routing.ForeachRouter.MAP_NOT_SUPPORTED_MESSAGE;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.BLOCKING;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.NON_BLOCKING;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.ScopeFeature.ForeachStory.FOR_EACH;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(SCOPE)
@Story(FOR_EACH)
@Issue("W-12207110")
public class ForeachFeatureFlagForMapExpressionsTestCase extends AbstractForeachTestCase {

  private final boolean featureFlagEnabled;
  @Rule
  public SystemProperty featureFlagEnabledProperty;

  public ForeachFeatureFlagForMapExpressionsTestCase(Mode mode, boolean featureFlagEnabled) {
    super(mode);
    this.featureFlagEnabled = featureFlagEnabled;
    this.featureFlagEnabledProperty =
        new SystemProperty(FOREACH_ROUTER_REJECTS_MAP_EXPRESSIONS_PROPERTY, valueOf(featureFlagEnabled));
  }

  @Parameterized.Parameters(name = "Mode: {0}, FeatureFlagEnabled: {1}")
  public static Collection<Object[]> parameters() {
    return asList(
                  new Object[] {BLOCKING, true},
                  new Object[] {NON_BLOCKING, true},
                  new Object[] {BLOCKING, false},
                  new Object[] {NON_BLOCKING, false});
  }

  @Test
  public void expectExceptionWhenFeatureFlagIsEnabled() throws Exception {
    if (featureFlagEnabled) {
      expectedException.expectMessage(MAP_NOT_SUPPORTED_MESSAGE);
      expectedException.expect(IllegalArgumentException.class);
    }
    process(simpleForeach, eventBuilder(muleContext).message(of(singletonMap("foo", "bar"))).build());
  }
}
