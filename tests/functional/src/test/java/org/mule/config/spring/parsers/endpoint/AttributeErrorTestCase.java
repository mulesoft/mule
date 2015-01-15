/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.endpoint;

import org.mule.module.springconfig.parsers.AbstractBadConfigTestCase;

import org.junit.Test;

public class AttributeErrorTestCase extends AbstractBadConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/endpoint/attribute-error-test-flow.xml";
    }

    @Test
    public void testError() throws Exception
    {
        assertErrorContains("do not match the exclusive groups [address] [ref]");
    }

}
