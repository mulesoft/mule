/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
