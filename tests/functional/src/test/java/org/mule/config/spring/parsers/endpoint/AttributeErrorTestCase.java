/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

import org.mule.config.spring.parsers.AbstractBadConfigTestCase;

public class AttributeErrorTestCase extends AbstractBadConfigTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/attribute-error-test.xml";
    }

    public void testError() throws Exception
    {
        assertErrorContains("The attribute 'ref' cannot appear with the attribute 'address'");
    }

}
