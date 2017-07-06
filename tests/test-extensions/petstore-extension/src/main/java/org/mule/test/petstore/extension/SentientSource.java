/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.FlowInfo;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

public class SentientSource extends Source<FlowInfo, Void> {

  public static Object capturedFlowInfo;

  private FlowInfo flowInfo;

  @Override
  public void onStart(SourceCallback<FlowInfo, Void> sourceCallback) throws MuleException {
    capturedFlowInfo = flowInfo;
  }

  @Override
  public void onStop() {
    capturedFlowInfo = null;
  }
}
