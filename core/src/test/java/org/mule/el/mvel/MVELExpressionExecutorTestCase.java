/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.el.context.AbstractELTestCase;
import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

@SmallTest
public class MVELExpressionExecutorTestCase extends AbstractELTestCase
{

    protected MVELExpressionExecutor mvel;
    protected MVELExpressionLanguageContext context;

    public MVELExpressionExecutorTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Before
    public void setupMVEL() throws InitialisationException
    {
        mvel = new MVELExpressionExecutor(new ParserConfiguration());
        context = Mockito.mock(MVELExpressionLanguageContext.class);
        Mockito.when(context.isResolveable(Mockito.anyString())).thenReturn(false);
    }

    @Test
    public void evaluateReturnInt()
    {
        assertEquals(4, mvel.execute("2*2", null));
    }

    @Test
    public void evaluateReturnString()
    {
        assertEquals("hi", mvel.execute("'hi'", null));
    }

    @Test(expected = CompileException.class)
    public void evaluateInvalidExpression()
    {
        assertEquals(4, mvel.execute("2*'2", null));
    }

    @Test(expected = CompileException.class)
    public void invalidExpression()
    {
        mvel.validate("a9-#'");
    }

    @Test
    public void validExpression()
    {
        mvel.validate("var a = 2");
    }

    @Test
    public void useContextClassLoader() throws ClassNotFoundException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(new MyClassClassLoader());
            assertFalse((Boolean) mvel.execute("1 is org.MyClass", null));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    public void safeMapPropertyAccessIsEnabled()
    {
        assertEquals(null, mvel.execute("['test1' : null].doesntExist", context));
    }

    @Test
    public void safeMapNestedPropertyAccessIsEnabled()
    {
        assertEquals(null, mvel.execute("['test1' : null].test1", context));
    }

    @Test
    public void safeBeanPropertyAccessIsEnabled()
    {
        assertNull(mvel.execute("new Object().doesntExist", context));
    }

    @Test
    public void safeNestedBeanPropertyAccessIsEnabled()
    {
        assertNull(mvel.execute("new Object().doesntExist.?other", context));
    }

    @Test(expected = RuntimeException.class)
    public void safeNestedBeanPropertyAccessMaintainsNullSafeBehavior()
    {
        assertNull(mvel.execute("new Object().doesntExist.other", context));
    }

    @Test(expected = RuntimeException.class)
    public void safeNestedMapPropertyAccessMaintainsNullSafeBehavior()
    {
        assertNull(mvel.execute("['test1' : null].doesntExist.other", context));
    }

    @Test(expected = RuntimeException.class)
    public void safePropertyDoesntMessNullSafeMode()
    {
        assertNull(mvel.execute("null.doesntExist", context));
    }

    @Test(expected = RuntimeException.class)
    public void invalidMethodCallFails()
    {
        assertNull(mvel.execute("new Object().doesntExist()", context));
    }

    static class MyClassClassLoader extends ClassLoader
    {
        @Override
        protected Class<?> findClass(String className) throws ClassNotFoundException
        {
            if (className.equals("org.MyClass"))
            {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "org/MyClass", null, "java/lang/Object", null);
                return defineClass(className, cw.toByteArray(), 0, cw.toByteArray().length);
            }
            else
            {
                return super.findClass(className);
            }
        }
    }
}
