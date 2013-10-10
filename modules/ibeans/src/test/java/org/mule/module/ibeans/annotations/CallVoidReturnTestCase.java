/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.annotations;

import org.ibeans.annotation.IntegrationBean;
import org.junit.Test;

public class CallVoidReturnTestCase extends AbstractIBeansTestCase
{
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
