/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jnp;

import org.mule.transport.AbstractFunctionalTestCase;

public class JnpFunctionalTestCase extends AbstractFunctionalTestCase
{

    public JnpFunctionalTestCase()
    {
        this.prefix = "jnp";
    }

    @Override
    protected String getConfigFile()
    {
        return "jnp-functional-test-flow.xml";
    }
}
