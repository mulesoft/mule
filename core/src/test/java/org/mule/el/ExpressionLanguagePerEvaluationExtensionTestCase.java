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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.el.ExpressionLanguagePerEvaluationExtension;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.el.context.AppContext;
import org.mule.el.context.MessageContext;
import org.mule.el.mvel.MVELExpressionLanguage;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class ExpressionLanguagePerEvaluationExtensionTestCase extends ExpressionLanguageExtensionTestCase
{

    public ExpressionLanguagePerEvaluationExtensionTestCase(Variant variant)
    {
        super(variant);
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SimpleConfigurationBuilder(Collections.singletonMap("key1", new TestExtension()));
    }

    @Test
    public void testMessageShortcutVariable() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        MuleMessage message = new DefaultMuleMessage("foo", muleContext);

        Assert.assertEquals("foo", mvel.evaluate("p", message));
    }

    class TestExtension implements ExpressionLanguagePerEvaluationExtension
    {

        @Override
        public void configureContext(ExpressionLanguageContext context)
        {
            context.importClass(Calendar.class);
            context.importClass("CAL", Calendar.class);
            try
            {
                context.importStaticMethod("dateFormat",
                    DateFormat.class.getMethod("getInstance", new Class[]{}));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            context.addVariable("a", "hi");
            context.addFinalVariable("b", "hi");
            context.addVariable("appShortcut", context.getVariable("app"));
            if (context.contains("message"))
            {
                context.addVariable("p", context.getVariable("message", MessageContext.class).getPayload());
            }
            context.addAlias("p2", "message.payload");
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
