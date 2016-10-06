/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.fail;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.runtime.core.api.MuleContext;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ConflictedHttpsTlsConfigTestCase extends CompatibilityFunctionalTestCase {

  private int configNumber;

  @Override
  protected MuleContext createMuleContext() throws Exception {
    return null;
  }

  @Override
  protected String getConfigFile() {
    return "conflicted-https-config-" + configNumber + ".xml";
  }

  @Test
  public void testConfigs() throws Exception {
    for (configNumber = 1; configNumber <= 3; configNumber++) {
      try {
        super.createMuleContext();
        fail("No conflict seen");
      } catch (Exception ex) {
        assertExceptionIsOfType(ex, CheckExclusiveAttributes.CheckExclusiveAttributesException.class);
      }
    }
  }

  public void assertExceptionIsOfType(Throwable ex, Class<? extends Throwable> type) {
    Set<Throwable> seen = new HashSet<>();

    while (true) {
      if (type.isInstance(ex)) {
        return;
      } else if (ex == null || seen.contains(ex)) {
        fail("Bad exception type");
      } else {
        seen.add(ex);
        ex = ex.getCause();
      }
    }
  }
}


