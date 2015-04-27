/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.authentication;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.internal.request.DefaultHttpAuthentication;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class BasicAuthenticationBuilderTestCase extends AbstractMuleTestCase
{

    public static final String PASSWORD = "password";
    public static final String USERNAME = "username";

    @Test
    public void basicConfig()
    {
        DefaultHttpAuthentication authentication = (DefaultHttpAuthentication) new BasicAuthenticationBuilder()
                .setPassword(PASSWORD).setUsername(USERNAME).build();
        assertThat(authentication.getPassword(), is(PASSWORD));
        assertThat(authentication.getUsername(), is(USERNAME));
    }


}