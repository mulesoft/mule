/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

@SmallTest
public class MVELExpressionExecutorTestCase extends AbstractMuleTestCase
{
    protected MVELExpressionExecutor mvel;
    protected MVELExpressionLanguageContext context;

    @Before
    public void setupMVEL() throws InitialisationException
    {
        mvel = new MVELExpressionExecutor(new ParserContext());
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
