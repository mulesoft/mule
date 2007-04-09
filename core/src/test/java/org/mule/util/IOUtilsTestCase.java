/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;

import java.io.InputStream;

public class IOUtilsTestCase extends AbstractMuleTestCase
{

    public void testLoadingResourcesAsStream() throws Exception
    {
        InputStream is = IOUtils.getResourceAsStream("test-dummy.properties", getClass(), false, false);
        assertNotNull(is);

        is = IOUtils.getResourceAsStream("test-dummyX.properties", getClass(), false, false);
        assertNull(is);
    }

}
