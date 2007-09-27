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

public class SedaPipelineNoPoolingTestCase extends SedaPipelineTestCase
{
    //@java.lang.Override
    protected void doPostFunctionalSetUp() throws Exception
    {
        //TODO: this should be configurable from the XML in Mule 2.0
        SedaModel model = (SedaModel) managementContext.getRegistry().lookupModel("main");
        model.setEnablePooling(false);
    }


}
