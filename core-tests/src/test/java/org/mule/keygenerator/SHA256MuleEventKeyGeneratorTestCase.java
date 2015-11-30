/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.keygenerator;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.NotSerializableException;

import org.junit.Test;

@SmallTest
public class SHA256MuleEventKeyGeneratorTestCase extends AbstractMuleContextTestCase
{

    private static final String TEST_INPUT = "TEST";

    private static final String TEST_HASH = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2";

    private SHA256MuleEventKeyGenerator keyGenerator = new SHA256MuleEventKeyGenerator();

    @Test
    public void generatesKeyApplyingSHA256ToPayload() throws Exception
    {
        MuleEvent event = getTestEvent(TEST_INPUT);
        String key = (String) keyGenerator.generateKey(event);
        assertEquals(TEST_HASH, key);
    }

    @Test(expected = NotSerializableException.class)
    public void failsToGenerateKeyWhenCannotReadPayload() throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        when(event.getMessageAsBytes()).thenThrow(new DefaultMuleException("Fail"));
        keyGenerator.generateKey(event);
    }

}
