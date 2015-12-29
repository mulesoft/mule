/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.watermark;

import org.mule.api.config.ConfigurationException;
import org.mule.functional.junit4.ApplicationContextBuilder;

import org.junit.Test;

public class WatermarkInvalidExpressionTestCase
{

    @Test(expected = ConfigurationException.class)
    public void invalidUpdateExpression() throws Exception
    {
        ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setApplicationResources(new String[] {"org/mule/test/integration/watermark/watermark-invalid-expression-config.xml"});
        builder.build();
    }

}
