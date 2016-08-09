/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.infrastructure.process.rules;

import org.mule.test.infrastructure.process.MuleProcessController;

import org.junit.rules.ExternalResource;

/**
 * This is a JUnit rule to start and stop Mule Runtime during tests. Usage:
 * <p/>
 * 
 * <pre>
 * public static class MuleStandaloneIntegrationTests {
 * 
 *   &#064;Rule
 *   public MuleStandalone standalone = new MuleStandalone(&quot;/path/to/mule/home&quot;);
 *
 *   &#064;Test
 *   public void integrationTest() throws IOException {
 *     MuleProcessController mule = standalone.getMule();
 *     assertThat(mule.isRunning(), is(true));
 *   }
 * }
 * </pre>
 */
public class MuleStandalone extends ExternalResource {

  private final MuleProcessController mule;
  private String[] args;

  public MuleStandalone(String muleHome) {
    mule = new MuleProcessController(muleHome);
  }

  public MuleStandalone(String muleHome, String... args) {
    mule = new MuleProcessController(muleHome);
    this.args = args;
  }

  @Override
  protected void before() throws Throwable {
    mule.start(args);
  }

  @Override
  protected void after() {
    mule.stop();
  }

  public MuleProcessController getMule() {
    return mule;
  }


}
