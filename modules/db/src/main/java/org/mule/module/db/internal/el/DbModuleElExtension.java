/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.el;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

/**
 * A {@link ExpressionLanguageExtension} which provides functions to use with the DB connector.
 */
public class DbModuleElExtension extends org.mule.el.mvel.DataConversion
        implements ExpressionLanguageExtension, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private DbCreateArrayFunction dbCreateArrayFunction;
    private DbCreateStructFunction dbCreateStructFunction;

    @Override
    public void initialise() throws InitialisationException
    {
        dbCreateArrayFunction = new DbCreateArrayFunction(muleContext);
        dbCreateStructFunction = new DbCreateStructFunction(muleContext);
    }

    @Override
    public void configureContext(ExpressionLanguageContext context)
    {
        context.declareFunction(DbCreateArrayFunction.DB_CREATE_ARRAY_FUNCTION, dbCreateArrayFunction);
        context.declareFunction(DbCreateStructFunction.DB_CREATE_STRUCT_FUNCTION, dbCreateStructFunction);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
