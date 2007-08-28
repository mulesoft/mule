/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.FunctionalTestCase;

public class DefaultThreadingProfileMule2083TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "default-threading-profile-1-mule-2083.xml, default-threading-profile-2-mule-2083.xml";
    }

    public void testStartup()
    {
        // no-op
    }

}
