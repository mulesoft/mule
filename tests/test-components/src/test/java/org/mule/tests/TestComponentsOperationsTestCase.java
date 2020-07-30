package org.mule.tests;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import org.junit.Test;

public class TestComponentsOperationsTestCase extends MuleArtifactFunctionalTestCase {

  /**
   * Specifies the mule config xml with the flows that are going to be executed in the tests, this file lives in the test resources.
   */
  @Override
  protected String getConfigFile() {
    return "test-mule-config.xml";
  }

  @Test
  public void executeEnqueueFlow() throws Exception {
    String payloadValue = ((String) flowRunner("enqueueFlow").withAttributes(singletonMap("someAttribute", "TheValue"))
            .run().getMessage().getPayload().getValue());
    assertThat(payloadValue, is("The payload"));
  }

  @Test
  public void executeLifecycleFlow() throws Exception {
    flowRunner("lifecycleFlow").run();
  }
}
