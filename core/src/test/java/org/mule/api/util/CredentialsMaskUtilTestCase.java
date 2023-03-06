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
import static org.mule.api.util.CredentialsMaskUtil.maskPassPhrase;
import static org.mule.api.util.CredentialsMaskUtil.maskUrlUserAndPassword;

import org.junit.Test;

public class CredentialsMaskUtilTestCase
{
    private static final String URL_TEST = "jdbc:sqlserver://1.1.1.1:1443;databaseName=STAGING;user=mulesoftuser;password=mulesoftpass;";
    private static final String EXPECTED_URL_TEST_MASKED = "jdbc:sqlserver://1.1.1.1:1443;databaseName=STAGING;user=<<user>>;password=<<credentials>>;";
    private static final String ENDPOINT_WITH_PASSPHRASE = "<sftp:outbound-endpoint passphrase=\"secretPassphrase\"></sftp:outbound-endpoint>";
    private static final String EXPECTED_ENDPOINT_WITH_MASKED_PASSPHRASE = "<sftp:outbound-endpoint passphrase=\"<<credentials>>\"></sftp:outbound-endpoint>";
    private static final String ENDPOINT_WITH_PASSPHRASE_BEFORE_ANGLE_BRACKET = "<sftp:outbound-endpoint passphrase=secretPassphrase></sftp:outbound-endpoint>";
    private static final String EXPECTED_ENDPOINT_WITH_MASKED_PASSPHRASE_BEFORE_ANGLE_BRACKET = "<sftp:outbound-endpoint passphrase=<<credentials>>></sftp:outbound-endpoint>";
    private static final String ENDPOINT_WITH_PASSPHRASE_BEFORE_WHITESPACE = "<sftp:outbound-endpoint passphrase=secretPassphrase ></sftp:outbound-endpoint>";
    private static final String EXPECTED_ENDPOINT_WITH_MASKED_PASSPHRASE_BEFORE_WHITESPACE = "<sftp:outbound-endpoint passphrase=<<credentials>> ></sftp:outbound-endpoint>";
    private static final String ENDPOINT_WITH_PASSPHRASE_BEFORE_SLASH = "<sftp:outbound-endpoint passphrase=pass/>";
    private static final String EXPECTED_ENDPOINT_WITH_PASSPHRASE_BEFORE_SLASH = "<sftp:outbound-endpoint passphrase=<<credentials>>/>";

    @Test
    public void whenUrlWithUserAndPasswordMaskUserPassword()
    {
        String maskedUrl = maskUrlUserAndPassword(URL_TEST, PASSWORD_PATTERN_NO_QUOTES, USER_PATTERN_NO_QUOTES);
        assertThat(maskedUrl, equalTo(EXPECTED_URL_TEST_MASKED));
    }

    @Test
    public void whenPassPhraseBeforeWhitespaceMaskPassPhrase()
    {
        String MASKED_STRING = maskPassPhrase(ENDPOINT_WITH_PASSPHRASE);
        assertThat(MASKED_STRING, equalTo(EXPECTED_ENDPOINT_WITH_MASKED_PASSPHRASE));
    }

    @Test
    public void whenPassPhraseBeforeAngleBracketMaskPassPhrase()
    {
        String MASKED_STRING = maskPassPhrase(ENDPOINT_WITH_PASSPHRASE_BEFORE_ANGLE_BRACKET);
        assertThat(MASKED_STRING, equalTo(EXPECTED_ENDPOINT_WITH_MASKED_PASSPHRASE_BEFORE_ANGLE_BRACKET));
    }

    @Test
    public void whenPassPhraseMaskPassPhrase()
    {
        String MASKED_STRING = maskPassPhrase(ENDPOINT_WITH_PASSPHRASE_BEFORE_WHITESPACE);
        assertThat(MASKED_STRING, equalTo(EXPECTED_ENDPOINT_WITH_MASKED_PASSPHRASE_BEFORE_WHITESPACE));
    }

    @Test
    public void whenPassPhraseBeforeSlashMaskPassPhrase()
    {
        String MASKED_STRING = maskPassPhrase(ENDPOINT_WITH_PASSPHRASE_BEFORE_SLASH);
        assertThat(MASKED_STRING, equalTo(EXPECTED_ENDPOINT_WITH_PASSPHRASE_BEFORE_SLASH));
    }
}
