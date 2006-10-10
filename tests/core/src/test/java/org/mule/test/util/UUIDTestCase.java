/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util;

import junit.framework.TestCase;

import org.mule.util.UUID;

public class UUIDTestCase extends TestCase
{

    public void testUUID()
    {
        assertNotNull(UUID.getUUID());
        String id = UUID.getUUID();
        assertNotNull(id);
        assertTrue(!id.equals(UUID.getUUID()));
    }

}
