/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.issues;

import org.mule.transport.AbstractFunctionalTestCase;

public class RmiMethodTypeMule1857TestCase extends AbstractFunctionalTestCase
{

    public RmiMethodTypeMule1857TestCase()
    {
        super("rmi", "rmi-method-type-1857-test.xml");
    }

}