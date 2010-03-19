/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.tck.AbstractMuleTestCase;

import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.Opcodes;

public class ExpressionTransformerTestCase extends AbstractMuleTestCase
{

    /**
     * See: MULE-4797 GroovyExpressionEvaluator script is unable to load user classes
     * when used with hot deployment.
     * 
     * @throws TransformerException
     */
    public void testExpressionEvaluationClassLoader() throws ClassNotFoundException, TransformerException
    {
        ExpressionTransformer transformer = new ExpressionTransformer();
        transformer.setMuleContext(muleContext);
        transformer.addArgument(new ExpressionArgument("test", new ExpressionConfig(
            "payload instanceof MyClass", "groovy", null), false));

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(new MyClassClassLoader());
            transformer.initialise();
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        assertFalse((Boolean) transformer.transform("test"));

    }

    class MyClassClassLoader extends ClassLoader
    {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            if (name.equals("MyClass"))
            {
                ClassWriter cw = new ClassWriter(true);
                cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "MyClass", null, "java/lang/Object", null);
                return defineClass(name, cw.toByteArray(), 0, cw.toByteArray().length);
            }
            else
            {
                return super.findClass(name);
            }
        }
    }
}
