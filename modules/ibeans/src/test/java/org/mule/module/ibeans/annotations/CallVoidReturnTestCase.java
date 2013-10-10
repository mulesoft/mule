/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

public class CallVoidReturnTestCase extends AbstractIBeansTestCase
{
    @SuppressWarnings("unused")
    @IntegrationBean
    private SearchIBean search;

    @Test
    public void testReturnVoid() throws Exception
    {
        if (isOffline(getClass().getName() + ".testReturnVoid"))
        {
            return;
        }

        //we just need to test that the call doesn't fail
        search.searchGoogleAndReturnVoid("foo");
    }

}
