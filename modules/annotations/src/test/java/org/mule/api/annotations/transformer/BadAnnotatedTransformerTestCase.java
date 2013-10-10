/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        Method m = getClass().getDeclaredMethod("voidTransformer", StringBuffer.class);
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
        Method m = getClass().getDeclaredMethod("privateTransformer", StringBuffer.class);
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
        Method m = getClass().getDeclaredMethod("protectedTransformer", StringBuffer.class);
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
        Method m = getClass().getDeclaredMethod("packageTransformer", StringBuffer.class);
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
        Method m = getClass().getDeclaredMethod("publicTransformerObjectReturn", StringBuffer.class);
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
        Method m = getClass().getDeclaredMethod("goodTransformer", StringBuffer.class);
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

    public void voidTransformer(StringBuffer in)
    {
        
    }

    public String noParamsTransformer()
    {
        return "";
    }

    private String privateTransformer(StringBuffer foo)
    {
        return foo.toString();
    }

    protected String protectedTransformer(StringBuffer foo)
    {
        return foo.toString();
    }

    String packageTransformer(StringBuffer foo)
    {
        return foo.toString();
    }

    public Object publicTransformerObjectReturn(StringBuffer foo)
    {
        return foo;
    }

    public String publicTransformerObjectParam(Object foo)
    {
        return foo.toString();
    }

    public String goodTransformer(StringBuffer foo)
    {
        return foo.toString();
    }

}
