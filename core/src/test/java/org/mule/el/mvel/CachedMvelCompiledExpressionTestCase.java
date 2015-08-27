/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import static org.junit.Assert.assertThat;
import org.mule.api.el.ExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

public class CachedMvelCompiledExpressionTestCase extends AbstractMuleContextTestCase
{

    public static final String VARIABLE = "VARIABLE";
    public static final String ASSIGNMENT_EXPRESSION = "payload.details=" + VARIABLE;

    @Test
    public void createsNewCompiledExpressionInstances() throws Exception
    {
        ExpressionLanguage expressionLanguage = muleContext.getExpressionLanguage();

        Map<String, Object> vars = new HashMap();
        vars.put(VARIABLE, new FooDetails());
        expressionLanguage.evaluate(ASSIGNMENT_EXPRESSION, getTestEvent(new Foo()), vars);


        BarDetails barDetails = new BarDetails();
        vars.put(VARIABLE, barDetails);
        Object result = expressionLanguage.evaluate(ASSIGNMENT_EXPRESSION, getTestEvent(new Bar()), vars);
        assertThat(result, Matchers.<Object>equalTo(barDetails));
    }

    public static class Foo
    {

        public FooDetails details;

    }

    public static class FooDetails
    {

    }

    public static class Bar
    {

        public BarDetails details;

    }

    public static class BarDetails
    {

    }
}
