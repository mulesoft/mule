/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.db.internal.util.DbCredentialsMaskUtil.maskUrlCredentialsPrefixed;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DbCredentialsMaskUtilTestCase extends AbstractMuleTestCase
{
    private static final String URL_TEST_PREFIXED = "jdbc:oracle:thin:a_visible_user/a_visible_password@localhost:49161:xe;";

    private static final String EXPECTED_URL_TEST_PREFIXED = "jdbc:<<credentials>>@localhost:49161:xe;";

    @Test
    public void whenUrlWithUserAndPasswordMaskUserPasswordPrefixed()
    {
        String maskedUrl = maskUrlCredentialsPrefixed(URL_TEST_PREFIXED);
        assertThat(maskedUrl, equalTo(EXPECTED_URL_TEST_PREFIXED));
    }

}
