/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.tck.AbstractMuleTestCase;

import org.apache.commons.lang.SerializationUtils;

public class VoidResultTestCase extends AbstractMuleTestCase
{

    public void testUniqueDeserialization()
    {
        VoidResult result = VoidResult.getInstance();

        byte[] serialized = SerializationUtils.serialize(result);
        assertNotNull(serialized);

        Object deserialized = SerializationUtils.deserialize(serialized);
        assertSame(deserialized, result);
        assertEquals(deserialized, result);
    }

}
