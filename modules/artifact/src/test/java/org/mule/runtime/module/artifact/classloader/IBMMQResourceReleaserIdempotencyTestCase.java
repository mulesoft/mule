/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import org.mule.module.artifact.classloader.IBMMQResourceReleaser;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Features({@Feature(LEAK_PREVENTION), @Feature(JAVA_SDK)})
@Stories({@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY), @Story(ARTIFACT_LIFECYCLE_LISTENER)})
@RunWith(Parameterized.class)
public class IBMMQResourceReleaserIdempotencyTestCase extends IBMMQResourceReleaserTestCase {

  public IBMMQResourceReleaserIdempotencyTestCase(String driverVersion) {
    super(driverVersion);
  }

  @Override
  protected void beforeClassLoaderDisposal() {
    super.beforeClassLoaderDisposal();
    new IBMMQResourceReleaser(artifactClassLoader).release();
    new IBMMQResourceReleaser(artifactClassLoader).release();
  }
}
