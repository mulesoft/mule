/*
 * $Id$
 *  --------------------------------------------------------------------------------------
 *  Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 *  The software in this package is published under the terms of the CPAL v1.0
 *  license, a copy of which has been included with this distribution in the
 *  LICENSE.txt file.
 */

package org.mule.config.pool;

import org.mule.api.config.ThreadingProfile;
import org.mule.tck.AbstractMuleTestCase;

public class DefaultThreadPoolFactoryTestCase extends AbstractMuleTestCase
{

    public void testDefaults() throws Exception
    {
        final ThreadingProfile tp = muleContext.getDefaultThreadingProfile();
        final ThreadPoolFactory pf = tp.getPoolFactory();
        assertTrue(pf instanceof DefaultThreadPoolFactory);
    }
}
