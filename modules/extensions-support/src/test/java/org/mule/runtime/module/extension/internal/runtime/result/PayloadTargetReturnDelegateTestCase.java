/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
    return new PayloadTargetReturnDelegate(TARGET, componentModel, muleContext);
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
