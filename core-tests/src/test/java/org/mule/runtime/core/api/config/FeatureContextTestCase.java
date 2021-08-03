/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FeatureFlaggingStory.FEATURE_FLAGGING;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@SmallTest
@io.qameta.allure.Feature(DEPLOYMENT_CONFIGURATION)
@Story(FEATURE_FLAGGING)
public class FeatureContextTestCase {

  private static final List<String> suffixedVersions = Arrays.asList("4.4.0-SNAPSHOT", "4.4.0-rc1", "4.4.0-rc1-SNAPSHOT");
  private static final MuleVersion nonSuffixedVersion = new MuleVersion("4.4.0");

  @Test
  @Issue("MULE-19682")
  public void testSemverSuffixesMustBeIgnored() {
    suffixedVersions.forEach(suffixedVersion -> {
      FeatureContext featureContext = new FeatureContext(new MuleVersion(suffixedVersion), "");
      assertThat(featureContext.getArtifactMinMuleVersion().get().sameAs(nonSuffixedVersion), is(true));
    });
  }

}
