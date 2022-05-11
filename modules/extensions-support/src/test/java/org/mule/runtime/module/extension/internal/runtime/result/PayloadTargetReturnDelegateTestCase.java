/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.result;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class PayloadTargetReturnDelegateTestCase extends TargetReturnDelegateTestCase {

  @Override
  protected ReturnDelegate createReturnDelegate() {
    return new PayloadTargetReturnDelegate(TARGET, componentModel, getCursorProviderFactory(), muleContext);
  }

  @Override
  protected Message getOutputMessage(CoreEvent result) {
    TypedValue<?> typedValue = result.getVariables().get(TARGET);
    return Message.builder().payload(typedValue).mediaType(typedValue.getDataType().getMediaType()).build();
  }

  @Override
  @Test
  public void operationReturnsOperationResultThatOnlySpecifiesPayloadAndAttributes() throws Exception {
    // this tests doesn't apply
  }
}
