/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;

import java.util.Arrays;

import groovyjarjarasm.asm.ClassWriter;
import groovyjarjarasm.asm.Opcodes;
import org.junit.Test;

public class GroovyExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testWithExpressions()
    {
        FruitBowl payload = createFruitBowl();
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);
        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);
        Object value = e.evaluate("payload.apple.washed", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue((Boolean) value);

        value = e.evaluate("message.payload.banana.bitten", msg);
        assertNotNull(value);
        assertTrue(value instanceof Boolean);
        assertTrue((Boolean) value);
    }

    @Test(expected = MuleRuntimeException.class)
    public void testThrowsExceptionOnScriptError() throws Exception
    {
        FruitBowl payload = createFruitBowl();
        DefaultMuleMessage msg = new DefaultMuleMessage(payload, muleContext);

        GroovyExpressionEvaluator e = new GroovyExpressionEvaluator();
        e.setMuleContext(muleContext);
        e.evaluate("unexistent", msg);
    }

    @Test
    public void testRegistrySyntax() throws Exception
    {
        Apple apple = new Apple();
        muleContext.getRegistry().registerObject("name.with.dots", apple);
        Object result = muleContext.getExpressionManager().evaluate(
                "#[groovy:registry.lookupObject('name.with.dots')]", null);

        assertNotNull(result);
        assertSame(apple, result);

        // try various map-style access in groovy for simpler syntax
        result = muleContext.getExpressionManager().evaluate(
            "#[groovy:registry.'name.with.dots']", null);
        assertNotNull(result);
        assertSame(apple, result);

        result = muleContext.getExpressionManager().evaluate(
                "#[groovy:registry['name.with.dots']]", null);
        assertNotNull(result);
        assertSame(apple, result);

        result = muleContext.getExpressionManager().evaluate(
                "#[groovy:registry.'name.with.dots'.washed]", null);
        assertNotNull(result);
        assertEquals(false, result);
    }

    @Test
    public void testComplexExpressionLowLevelParsing() throws Exception
    {
        final GroovyExpressionEvaluator evaluator = new GroovyExpressionEvaluator();
        evaluator.setMuleContext(muleContext);
        muleContext.getExpressionManager().registerEvaluator(evaluator);

        MuleMessage msg = new DefaultMuleMessage(Arrays.asList(0, "test"), muleContext);
        String result = muleContext.getExpressionManager().parse("#[groovy:payload[0]] - #[groovy:payload[1].toUpperCase()]",
                                                                 msg);

        assertNotNull(result);
        assertEquals("Expressions didn't evaluate correctly",
                     "0 - TEST", result);
    }

    /**
     * See: MULE-4797 GroovyExpressionEvaluator script is unable to load user classes
     * when used with hot deployment See:
     */
    @Test
    public void testUseContextClassLoaderToResolveClasses() throws ClassNotFoundException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(new MyClassClassLoader());
            assertFalse((Boolean) muleContext.getExpressionManager().evaluate(
                "groovy:payload instanceof MyClass", new DefaultMuleMessage("test", muleContext)));
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private FruitBowl createFruitBowl()
    {
        Apple apple = new Apple();
        apple.wash();
        Banana banana = new Banana();
        banana.bite();

        return new FruitBowl(apple, banana);
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
