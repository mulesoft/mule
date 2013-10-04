/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@SmallTest
public class NullPayloadTestCase extends AbstractMuleTestCase
{

    @Test
    public void testUniqueDeserialization()
    {
        NullPayload result = NullPayload.getInstance();

        byte[] serialized = SerializationUtils.serialize(result);
        assertNotNull(serialized);

        Object deserialized = SerializationUtils.deserialize(serialized);
        assertSame(deserialized, result);
        assertEquals(deserialized, result);
    }

}
