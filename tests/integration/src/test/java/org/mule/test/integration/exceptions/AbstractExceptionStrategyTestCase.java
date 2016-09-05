/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.functional.listener.ExceptionListener;
import org.mule.functional.listener.SystemExceptionListener;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.client.MuleClient;

public abstract class AbstractExceptionStrategyTestCase extends AbstractIntegrationTestCase {

  protected ExceptionListener exceptionListener;
  protected SystemExceptionListener systemExceptionListener;
  protected MuleClient client;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    if (client == null) {
      client = muleContext.getClient();
    }

    exceptionListener = new ExceptionListener(muleContext);
    systemExceptionListener = new SystemExceptionListener(muleContext);
  }

}


