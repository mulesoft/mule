/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
