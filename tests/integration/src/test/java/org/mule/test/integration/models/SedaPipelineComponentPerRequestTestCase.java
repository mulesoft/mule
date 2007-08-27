/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.models;

import org.mule.impl.model.seda.SedaModel;
import org.mule.umo.model.UMOModel;

public class SedaPipelineComponentPerRequestTestCase extends AbstractPipelineTestCase
{
    protected String getModelType()
    {
        return "seda";
    }

    protected void configureModel(UMOModel model)
    {
        SedaModel m = (SedaModel)model;
        m.setComponentPerRequest(true);
        m.setEnablePooling(false);
    }
}
