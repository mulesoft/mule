/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.db.integration.config;

import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class DatasourceWithConnectionPropertiesBeanConfigTestCase extends AbstractMuleContextTestCase
{

    @Test(expected = InitialisationException.class)
    public void expectFailure() throws Exception
    {
        //TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
        new DefaultMuleContextFactory().createMuleContext(new SpringXmlConfigurationBuilder("integration/config/bean-datasource-with-connection-properties-config.xml"));
    }

}
