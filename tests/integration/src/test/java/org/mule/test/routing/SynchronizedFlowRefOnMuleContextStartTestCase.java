/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;

public class SynchronizedFlowRefOnMuleContextStartTestCase extends AbstractSynchronizedMuleContextStartTestCase {

  @Override
  protected String getConfigFile() {
    return "synchronized-flowref-mule-context-start-config.xml";
  }


  public static class UnblockProcessingSource implements MessageSource, Startable {

    @Override
    public void start() throws MuleException {
      waitMessageInProgress.release();
    }

    @Override
    public void setListener(MessageProcessor listener) {

    }
  }
}
