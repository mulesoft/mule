/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getDefaultCursorStreamProviderFactory;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.size.SmallTest;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ValueReturnDelegateTestCase extends ValueReturnDelegateContractTestCase {

  @Override
  protected ReturnDelegate createReturnDelegate() {
    return new ValueReturnDelegate(componentModel, getDefaultCursorStreamProviderFactory(), muleContext);
  }

  @Override
  protected Message getOutputMessage(CoreEvent result) {
    Message message = result.getMessage();

    assertThat(message, is(notNullValue()));
    return message;
  }
}
