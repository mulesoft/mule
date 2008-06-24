/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.ThirdPartyContainer;
import org.mule.tck.FunctionalTestCase;

public class ThirdPartyTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/third-party-test.xml";
    }

    public void testContainer()
    {
        ThirdPartyContainer container = (ThirdPartyContainer) muleContext.getRegistry().lookupObject("container");
        assertNotNull(container);
        assertNotNull(container.getThing());
    }

}
