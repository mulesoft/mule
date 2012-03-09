/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.spi;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.el.mvel.MuleVariableResolverFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.el.context.AbstractELTestCase;
import org.mule.el.mvel.MVELExpressionLanguage;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.mvel2.ParserContext;

public class MVELExpressionLanguageStaticContributionTestCase extends AbstractELTestCase
{

    public MVELExpressionLanguageStaticContributionTestCase(Variant variant)
    {
        super(variant);
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SimpleConfigurationBuilder(Collections.singletonMap("key1", new TestStaticContribution()));
    }

    @Override
    protected ExpressionLanguage getExpressionLanguage() throws RegistrationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        return mvel;
    }

    @Test
    public void addImport() throws RegistrationException, InitialisationException
    {

        Assert.assertEquals(TestStaticContribution.class,
            expressionLanguage.evaluate("TestStaticContribution"));
    }

    @Test
    public void customStringVariable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("hi", expressionLanguage.evaluate("a"));
    }

    @Test
    public void assignValueToCustomStringVariable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("1", expressionLanguage.evaluate("a=1"));
    }

    @Test
    public void customFinalStringVariable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("hi", expressionLanguage.evaluate("b"));
    }

    @Test
    public void assignValueToCustomFinalStringVariable()
        throws RegistrationException, InitialisationException
    {
        assertImmutableVariable("b=1");
    }

    @Test
    public void testShortcutVariable() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        Assert.assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("appShortcut.name"));
    }

    class TestStaticContribution implements MVELExpressionLanguageExtension
    {

        @Override
        public void configureParserContext(ParserContext parserContext)
        {
            parserContext.addImport(TestStaticContribution.class);

        }

        @Override
        public void configureStaticVariableResolverFactory(MuleVariableResolverFactory resolverFactory)
        {
            resolverFactory.addVariable("a", "hi");
            resolverFactory.addFinalVariable("b", "hi");
            resolverFactory.addVariable("appShortcut", resolverFactory.getVariable("app"));
        }
    }
}
