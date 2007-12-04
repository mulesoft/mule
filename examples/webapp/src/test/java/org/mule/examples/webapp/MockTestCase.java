/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.webapp;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.WebappMuleXmlConfigurationBuilder;

import com.mockobjects.constraint.IsInstanceOf;
import com.mockobjects.dynamic.FullConstraintMatcher;
import com.mockobjects.dynamic.Mock;

import javax.servlet.ServletContext;

/**
 * This test just basically checks that all the config files used in the webapp example 
 * are valid and can co-exist without any conflicts.
 */
public class MockTestCase extends AbstractWebappTestCase
{
    //@Override
    protected String getConfigurationResources()
    {
        return //"jmx-config.xml," +
                "echo-config.xml," +
                "hello-http-config.xml," +
                "servlet-config.xml," +
                "stockquote-rest-config.xml," +
                "loan-broker-sync-config.xml," +
                "loan-broker-axis-endpoints-config.xml";
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        Mock context = new Mock(ServletContext.class);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        context.expectAndReturn("getResourceAsStream", new FullConstraintMatcher(new IsInstanceOf(String.class)), null);
        return new WebappMuleXmlConfigurationBuilder((ServletContext) context.proxy(), null);
    }
}
