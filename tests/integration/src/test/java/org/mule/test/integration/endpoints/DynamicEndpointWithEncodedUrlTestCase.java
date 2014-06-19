/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.endpoints;

public class DynamicEndpointWithEncodedUrlTestCase extends AbstractEndpointEncodedUrlTestCase
{

    @Override
    protected String getEncodedUrlConfigFile()
    {
        return "org/mule/test/integration/endpoints/dynamic-endpoint-encoded-url-config.xml";
    }

    @Override
    protected String getDynamicUrl()
    {
        return "vm://testDynamic";
    }

    @Override
    protected String getAssembledDynamicUrl()
    {
        return "vm://testAssembledDynamic";
    }

    @Override
    protected String getStaticUrl()
    {
        return "vm://testStatic";
    }

    @Override
    protected String getAssembledStaticUrl()
    {
        return "vm://testAssembledStatic";
    }
}
