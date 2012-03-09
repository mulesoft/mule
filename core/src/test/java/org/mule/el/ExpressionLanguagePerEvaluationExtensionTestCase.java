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
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguagePerEvaluationExtension;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transport.PropertyScope;
import org.mule.el.context.AbstractELTestCase;
import org.mule.el.context.MessageContext;
import org.mule.el.mvel.MVELExpressionLanguage;

import org.junit.Assert;
import org.junit.Test;

public class ExpressionLanguagePerEvaluationExtensionTestCase extends AbstractELTestCase
{

    public ExpressionLanguagePerEvaluationExtensionTestCase(Variant variant)
    {
        super(variant);
    }

    @Override
    protected ExpressionLanguage getExpressionLanguage() throws RegistrationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        muleContext.getRegistry().registerObject("key1", new TestDynamicContribution());
        return mvel;
    }

    @Test
    public void customStringVariable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("hi", expressionLanguage.evaluate("a"));
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
        muleContext.getRegistry().registerObject("key1", new TestDynamicContribution());
        mvel.initialise();
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);

        Assert.assertEquals("bar", mvel.evaluate("i['foo']", message));
    }

    class TestDynamicContribution implements ExpressionLanguagePerEvaluationExtension
    {

        @Override
        public void configureContext(ExpressionLanguageContext resolverFactory)
        {
            resolverFactory.addVariable("a", "hi");
            resolverFactory.addFinalVariable("b", "hi");
            if (resolverFactory.containsVariable("message"))
            {
                resolverFactory.addVariable("i",
                    ((MessageContext) resolverFactory.getVariable("message")).getInboundProperties());
            }
        }
    }
}
