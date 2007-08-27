/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.tck.FunctionalTestCase;

public class MultipleNamedConnectorsTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "multiple-named-connectors-test.xml";
    }

    public void testMultipleNamedConnectors() throws Exception
    {
        // no-op, the initialization must not fail.   
    }
}
