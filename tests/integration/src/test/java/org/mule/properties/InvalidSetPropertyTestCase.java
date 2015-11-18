/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;
import org.junit.Test;

public class InvalidSetPropertyTestCase extends AbstractFakeMuleServerTestCase
{
    @Test
    public void emptyPropertyName() throws Exception
    {
       muleServer.start();
       muleServer.deployAppFromClasspathFolder("org/mule/properties/invalid-set-property-app", "invalid-config");
       muleServer.assertDeploymentFailure("invalid-config");
    }
}
