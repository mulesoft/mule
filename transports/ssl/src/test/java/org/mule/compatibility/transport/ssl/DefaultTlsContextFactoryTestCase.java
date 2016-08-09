/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultTlsContextFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void failIfTrustStoreIsNonexistent() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
    expectedException.expect(IOException.class);
    expectedException.expectMessage(containsString("Resource non-existent-trust-store could not be found"));
    tlsContextFactory.setTrustStorePath("non-existent-trust-store");
  }

}
