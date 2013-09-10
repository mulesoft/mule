/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.MuleEvent;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.tck.size.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class OnNoTokenPolicyTestCase
{

    @Test(expected = NotAuthorizedException.class)
    public void exception() throws NotAuthorizedException
    {
        NotAuthorizedException e = new NotAuthorizedException("");
        try
        {
            OnNoTokenPolicy.EXCEPTION.handleNotAuthorized(null, e, null);
        }
        catch (NotAuthorizedException nae)
        {
            Assert.assertSame(e, nae);
            throw nae;
        }
    }

    @Test
    public void stopFlow() throws NotAuthorizedException
    {
        MuleEvent event = OnNoTokenPolicy.STOP_FLOW.handleNotAuthorized(Mockito.mock(OAuthAdapter.class),
            new NotAuthorizedException(""), Mockito.mock(MuleEvent.class));
        Assert.assertNull(event);
    }

}
