/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.test.infrastructure.process.rules.MuleDeployment.application;
import org.mule.test.infrastructure.process.rules.MuleDeployment;

import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class DummyAppTestCase
{

    @ClassRule
    public static MuleDeployment muleDeployment = application("/opt/mule-enterprise-standalone-4.0-SNAPSHOT/apps/default").withProperties(null).deploy();

    @Test
    public void test()
    {
        LoggerFactory.getLogger(this.getClass()).info("MULE_HOME: " + muleDeployment.getHome());
    }

}
