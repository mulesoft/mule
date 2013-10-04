/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SmallTest
public class IOUtilsTestCase extends AbstractMuleTestCase
{

    @Test
    public void testLoadingResourcesAsStream() throws Exception
    {
        InputStream is = IOUtils.getResourceAsStream("log4j.properties", getClass(), false, false);
        assertNotNull(is);

        is = IOUtils.getResourceAsStream("does-not-exist.properties", getClass(), false, false);
        assertNull(is);
    }

}
