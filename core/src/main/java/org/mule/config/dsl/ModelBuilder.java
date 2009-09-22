/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.model.Model;
import org.mule.model.seda.SedaModel;

/**
 * TODO
 */
public class ModelBuilder
{
    private MuleContext muleContext;

    private Model model;

    public ModelBuilder(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        model = new SedaModel();
    }

    public ServiceBuilder service(String name)
    {
        ServiceBuilder builder = new ServiceBuilder(name, muleContext);
        builder.getService().setModel(model);
        return builder;
    }
}
