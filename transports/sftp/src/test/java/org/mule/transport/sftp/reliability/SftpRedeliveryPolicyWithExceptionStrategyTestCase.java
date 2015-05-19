/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.reliability;

import java.util.List;

import org.junit.runners.Parameterized;

public class SftpRedeliveryPolicyWithExceptionStrategyTestCase extends AbstractSftpRedeliveryTestCase
{

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters()
    {
        return getParameters();
    }

    public SftpRedeliveryPolicyWithExceptionStrategyTestCase(String name, boolean archive)
    {
        super(name, archive);
    }

    @Override
    protected String getConfigFile()
    {
        return "sftp-redelivery-with-catch-strategy.xml";
    }

}
