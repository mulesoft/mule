/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal.connectivity;

import static org.mule.test.allure.AllureConstants.ToolingSupport.TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.ToolingSupport.ServiceBuilderStory.SERVICE_BUILDER;

import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.internal.AbstractArtifactAgnosticServiceBuilderTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(TOOLING_SUPPORT)
@Story(SERVICE_BUILDER)
@Issue("W-13057814")
public class DefaultConnectivityTestingServiceBuilderTestCase extends AbstractArtifactAgnosticServiceBuilderTestCase {

  @Override
  protected ArtifactAgnosticServiceBuilder getArtifactAgnosticServiceBuilder(DefaultApplicationFactory applicationFactory) {
    return new DefaultConnectivityTestingServiceBuilder(applicationFactory);
  }

}
