/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
