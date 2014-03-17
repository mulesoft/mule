/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.transformer;

import org.mule.config.transformer.AnnotatedTransformerProxy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.fail;

public class BadAnnotatedTransformerTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testVoidTransformer() throws Exception
    {
        Method m = getClass().getDeclaredMethod("voidTransformer", StringBuilder.class);
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testNoParamsTransformer() throws Exception
    {
        Method m = getClass().getDeclaredMethod("noParamsTransformer", new Class[]{});
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testPrivateTransformer() throws Exception
    {
        Method m = getClass().getDeclaredMethod("privateTransformer", StringBuilder.class);
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testProtectedTransformer() throws Exception
    {
        Method m = getClass().getDeclaredMethod("protectedTransformer", StringBuilder.class);
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testPackageTransformer() throws Exception
    {
        Method m = getClass().getDeclaredMethod("packageTransformer", StringBuilder.class);
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testPublicTransformerObjectReturn() throws Exception
    {
        Method m = getClass().getDeclaredMethod("publicTransformerObjectReturn", StringBuilder.class);
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testPublicTransformerObjectParam() throws Exception
    {
        Method m = getClass().getDeclaredMethod("publicTransformerObjectParam", Object.class);
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, m.getParameterTypes(), null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testGoodTransformerWithObjectSource() throws Exception
    {
        Method m = getClass().getDeclaredMethod("goodTransformer", StringBuilder.class);
        Class c[] = new Class[]{String.class, Object.class};
        try
        {
            new AnnotatedTransformerProxy(5, getClass(), m, c, null, null);
            fail("Cannot register invalid transformer method");
        }
        catch (IllegalArgumentException e)
        {
            //Expected
        }
    }

    @Test
    public void testBadTransformerRegistration() throws Exception
    {
        try
        {
            muleContext.getRegistry().registerObject("badTransformer", new BadAnnotatedTransformer());
            fail("Cannot register invalid transformer method");            
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }

    }

    public void voidTransformer(StringBuilder in)
    {
        
    }

    public String noParamsTransformer()
    {
        return "";
    }

    private String privateTransformer(StringBuilder foo)
    {
        return foo.toString();
    }

    protected String protectedTransformer(StringBuilder foo)
    {
        return foo.toString();
    }

    String packageTransformer(StringBuilder foo)
    {
        return foo.toString();
    }

    public Object publicTransformerObjectReturn(StringBuilder foo)
    {
        return foo;
    }

    public String publicTransformerObjectParam(Object foo)
    {
        return foo.toString();
    }

    public String goodTransformer(StringBuilder foo)
    {
        return foo.toString();
    }

}
