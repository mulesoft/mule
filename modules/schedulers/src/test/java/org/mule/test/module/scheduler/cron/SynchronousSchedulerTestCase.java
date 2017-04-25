/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scheduler.cron;


import static junit.framework.Assert.assertEquals;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * <p>
 * Validates that a synchronous flow processing strategy implies a synchronous poll execution
 * </p>
 */
public class SynchronousSchedulerTestCase extends MuleArtifactFunctionalTestCase {

  private static List<String> foo = new ArrayList<String>();

  @Override
  protected String getConfigFile() {
    return "cron-synchronous-scheduler-config.xml";
  }

  @Test
  public void test() throws InterruptedException {
    Thread.sleep(6000);

    assertEquals(1, foo.size());
  }


  public static class FooComponent {

    public boolean process(String s) {
      synchronized (foo) {

        foo.add(s);

      }
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {

      }
      return false;
    }
  }
}
