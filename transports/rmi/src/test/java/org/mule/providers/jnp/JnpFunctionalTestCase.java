/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jnp;

import org.mule.providers.AbstractFunctionalTestCase;

public class JnpFunctionalTestCase extends AbstractFunctionalTestCase
{

    public JnpFunctionalTestCase()
    {
        super("jnp", "jnp-functional-test.xml");
    }

}
