/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.model.Model;
import org.mule.config.i18n.CoreMessages;
import org.mule.model.seda.SedaModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class ModelBuilder
{
    protected transient Log logger = LogFactory.getLog(getClass());
    
    private MuleContext muleContext;

    private Model model;

    public ModelBuilder(MuleContext muleContext)
    {
        logger.warn(CoreMessages.modelDeprecated());
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
