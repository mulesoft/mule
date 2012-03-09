/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.el.context.AbstractELTestCase;
import org.mule.el.context.AppContext;
import org.mule.el.mvel.MVELExpressionLanguage;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class ExpressionLanguageExtensionTestCase extends AbstractELTestCase
{

    public ExpressionLanguageExtensionTestCase(Variant variant)
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

    @Test
    public void testFunction() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        Assert.assertEquals("called param[0]=one,param[1]=two,app.name="
                            + muleContext.getConfiguration().getId(), mvel.evaluate("f('one','two')"));
    }

    class TestStaticContribution implements ExpressionLanguageExtension
    {

        @Override
        public void configureContext(ExpressionLanguageContext context)
        {
            context.importClass(TestStaticContribution.class);
            context.addVariable("a", "hi");
            context.addFinalVariable("b", "hi");
            context.addVariable("appShortcut", context.getVariable("app"));
            context.declareFunction("f", new ExpressionLanguageFunction()
            {

                @Override
                public void validateParams(Object[] params)
                {
                    if (params.length != 2)
                    {
                        throw new RuntimeException();
                    }
                }

                @Override
                public Object call(Object[] params, ExpressionLanguageContext context)
                {
                    return "called param[0]=" + params[0] + ",param[1]=" + params[1] + ",app.name="
                           + ((AppContext) context.getVariable("app")).getName();
                }
            });
        }
    }
}
