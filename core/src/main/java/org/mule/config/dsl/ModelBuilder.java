/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        builder.create().setModel(model);
        return builder;
    }
}
