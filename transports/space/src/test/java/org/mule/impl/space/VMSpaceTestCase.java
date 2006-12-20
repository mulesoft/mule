/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.space;

public class VMSpaceTestCase extends AbstractLocalSpaceTestCase
{

    protected DefaultSpaceFactory getSpaceFactory() throws Exception
    {
        return new VMSpaceFactory(false);
    }

    protected boolean isPersistent()
    {
        return false;
    }

}
