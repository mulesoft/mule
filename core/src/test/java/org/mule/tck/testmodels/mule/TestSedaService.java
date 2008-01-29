/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleException;
import org.mule.model.seda.SedaService;


/**
 * Exposes some internals of the SedaService useful for unit testing.
 */
public class TestSedaService extends SedaService
{
    public TestSedaService()
    {
        super();
    }

    //@Override
    public Object getOrCreateService() throws MuleException
    {
        return super.getOrCreateService();
    }
}
