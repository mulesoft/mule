/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.test.marvel.model.Relic;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
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
    return Arrays.asList(new Object[][] {{"Repeatable items", "repeatable-iterable-items-config.xml", true},
        {"Non repeatable items", "non-repeatable-iterable-items-config.xml", false}});
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
