/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import org.junit.Test;

public class RootServletTestCase extends AbstractServletTestCase
{

    public RootServletTestCase() 
    {
        super("/*");
    }

    @Override
    protected String getConfigResources()
    {
        return "servlet-conf.xml";
    }

    @Test
    public void testBasic() throws Exception
    {
        doTestBasic("http://localhost:63088/base");
    }
}
