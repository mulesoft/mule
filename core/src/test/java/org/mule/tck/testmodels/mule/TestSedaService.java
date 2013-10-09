/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;
import org.mule.model.seda.SedaService;


/**
 * Exposes some internals of the SedaService useful for unit testing.
 */
public class TestSedaService extends SedaService
{
    public TestSedaService(MuleContext muleContext)
    {
        super(muleContext);
    }
}
