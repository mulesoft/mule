package org.mule.tests;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tests.api.LifecycleTrackerRegistry;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Test;

public class TestComponentsOperationsTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  private LifecycleTrackerRegistry trackerRegistry;

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
    assertThat(trackerRegistry.get("first").getCalledPhases(), containsInAnyOrder("setMuleContext", "initialise", "start"));
    assertThat(trackerRegistry.get("second").getCalledPhases(), containsInAnyOrder("setMuleContext", "initialise", "start"));
  }

  @Test
  public void globalLifecycleObject() {
    assertThat(trackerRegistry.get("global").getCalledPhases(), containsInAnyOrder("setMuleContext", "initialise", "start"));
  }
}
