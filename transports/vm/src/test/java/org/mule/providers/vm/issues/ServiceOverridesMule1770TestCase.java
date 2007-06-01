/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm.issues;

import org.mule.tck.FunctionalTestCase;

public class ServiceOverridesMule1770TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "service-overrides-mule-1770-test.xml";
    }

    public void testStartsOk()
    {
        // that's all folks
    }

}
