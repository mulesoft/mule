/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.keygenerator;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.NotSerializableException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class SHA256MuleEventKeyGeneratorTestCase extends AbstractMuleContextTestCase {

  private static final String TEST_INPUT = "TEST";

  private static final String TEST_HASH = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2";

  private SHA256MuleEventKeyGenerator keyGenerator = new SHA256MuleEventKeyGenerator();

  @Before
  public void before() {
    keyGenerator.setMuleContext(muleContext);
  }

  @Test
  public void generatesKeyApplyingSHA256ToPayload() throws Exception {
    String key = (String) keyGenerator.generateKey(eventBuilder().message(of(TEST_INPUT)).build());
    assertEquals(TEST_HASH, key);
  }

  @Test(expected = NotSerializableException.class)
  public void failsToGenerateKeyWhenCannotReadPayload() throws Exception {
    Event event = mock(Event.class);
    when(event.getMessageAsBytes(muleContext)).thenThrow(new DefaultMuleException("Fail"));
    keyGenerator.generateKey(event);
  }

}
