/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.employee;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import org.junit.Rule;
import org.junit.Test;

public class MtomClientTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "mtom-client-conf-flow-httpn.xml";
  }

  @Test
  public void testEchoService() throws Exception {
    final EmployeeDirectoryImpl svc = (EmployeeDirectoryImpl) getComponent("employeeDirectoryService");

    Prober prober = new PollingProber(6000, 500);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(svc.getInvocationCount(), is(greaterThanOrEqualTo(1)));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Expected invocation count to be at least 1.";
      }
    });

    // ensure that an attachment was actually sent.
    assertTrue(AttachmentVerifyInterceptor.HasAttachments);
  }

}

