/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import static org.mule.api.config.MuleProperties.MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE;

import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class WssCheckTimestampInResponseTestCase extends AbstractWssCheckTimestampInResponseTestCase
{

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                                             // TS in response and TS check action requested
                                             {new SystemProperty(MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE, Boolean.TRUE.toString()),
                                              EXPECTED_RESPONSE, dynamicPortTSInResponseTSCheck, ECHO_REQUEST_WITH_HEADERS},
                                             // No TS in response and TS check action requested
                                             {new SystemProperty(MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE, Boolean.TRUE.toString()),
                                              EXPECTED_ERROR_NO_TIMESTAMP_RESPONSE, dynamicPortNoTSInResponseTSCheck, ECHO_REQUEST_WITH_HEADERS},
                                             // TS in response and no TS check action requested
                                             {new SystemProperty(MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE, Boolean.FALSE.toString()),
                                              EXPECTED_ERROR_NO_TIMESTAMP_RESPONSE, dynamicPortTSInResponseNoTSCheck, ECHO_REQUEST_WITH_HEADERS},
                                             // No TS in response and No TS check action requested
                                             {new SystemProperty(MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE, Boolean.FALSE.toString()),
                                              EXPECTED_RESPONSE, dynamicPortNoTSInResponseNoTSCheck, ECHO_REQUEST_WITH_HEADERS}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return "wss-timestamp-in-response-test-case.xml";
    }

}
