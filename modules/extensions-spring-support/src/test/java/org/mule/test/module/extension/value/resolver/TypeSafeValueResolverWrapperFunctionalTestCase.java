/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
