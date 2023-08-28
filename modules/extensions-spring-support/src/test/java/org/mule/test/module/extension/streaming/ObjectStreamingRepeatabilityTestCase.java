/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.streaming;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.test.marvel.model.Relic;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ObjectStreamingRepeatabilityTestCase extends AbstractStreamingExtensionTestCase {

  @Parameterized.Parameter
  public String testName;

  @Parameterized.Parameter(1)
  public String configFile;

  @Parameterized.Parameter(2)
  public boolean shouldBeRepeatable;


  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Repeatable items", "streaming/repeatable-iterable-items-config.xml", true},
        {"Non repeatable items", "streaming/non-repeatable-iterable-items-config.xml", false}});
  }

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void withNonRepeatableIterable() throws Exception {
    Relic relic = (Relic) flowRunner("firstElementOfNonRepeatableIterable").run().getMessage().getPayload().getValue();
    assertValueRepeatability(relic.getDescription(), shouldBeRepeatable);
  }

  @Test
  public void withRepeatableIterable() throws Exception {
    Relic relic = (Relic) flowRunner("firstElementOfRepeatableIterable").run().getMessage().getPayload().getValue();
    assertValueRepeatability(relic.getDescription(), true);
  }

  protected void assertValueRepeatability(Object value, boolean shouldBeRepeatable) {
    if (shouldBeRepeatable) {
      assertThat(value, instanceOf(CursorStreamProvider.class));
    } else {
      assertThat(value, not(instanceOf(CursorStreamProvider.class)));
    }
  }

}
