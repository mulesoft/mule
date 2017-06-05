/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.util.CredentialsMaskUtil.PASSWORD_PATTERN_NO_QUOTES;
import static org.mule.api.util.CredentialsMaskUtil.USER_PATTERN_NO_QUOTES;
import static org.mule.api.util.CredentialsMaskUtil.maskUserAndPassword;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class CredentialsMaskUtilTestCase extends AbstractMuleTestCase
{
    private static final String URL_TEST = "jdbc:sqlserver://1.1.1.1:1443;databaseName=STAGING;user=mulesoftuser;password=mulesoftpass;";

    private static final String EXPECTED_URL_TEST_MASKED = "jdbc:sqlserver://1.1.1.1:1443;databaseName=STAGING;user=<<user>>;password=<<credentials>>;";

    @Test
    public void whenUrlWithUserAndPasswordMaskUserPassword()
    {
        String maskedUrl = maskUserAndPassword(URL_TEST, PASSWORD_PATTERN_NO_QUOTES, USER_PATTERN_NO_QUOTES);
        assertThat(maskedUrl, equalTo(EXPECTED_URL_TEST_MASKED));
    }

}
