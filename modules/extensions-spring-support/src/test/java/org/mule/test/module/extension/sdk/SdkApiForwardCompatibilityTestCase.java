/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.sdk;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class SdkApiForwardCompatibilityTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "sdk/sdk-api-classloading-mule-config-flow.xml";
  }

  @Test
  public void sdkApiWithUnexpectedJavaVersions() throws Exception {
    assertThat(getExtensionModel("New Java versions").isPresent(), is(true));
    assertThat(getExtensionModel("New Java versions").get().getSupportedJavaVersions(),
               contains(JAVA_17.version(), JAVA_21.version()));

    String response = (String) flowRunner("healthCheck").run().getMessage().getPayload().getValue();
    assertThat(response, is("Extension running fine!"));
  }

}
