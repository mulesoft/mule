/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.tck.AbstractMuleTestCase;

import org.apache.commons.lang.SerializationUtils;

public class NullPayloadTestCase extends AbstractMuleTestCase
{

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
