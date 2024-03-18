/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.value.resolver;

import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {"org.mule.tests:mule-tests-model"})
public class TypeSafeValueResolverWrapperFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "heisenberg-stream-parameter.xml";
  }

  @Test
  public void testContentConvertedToStream() throws Exception {
    Message firstMessage = flowRunner("streamContent").run().getMessage();
    Message secondMessage = flowRunner("streamContent").run().getMessage();
    assertThat(firstMessage.getPayload().getValue(), is(secondMessage.getPayload().getValue()));
  }

}
