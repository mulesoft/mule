/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.config.MuleManifest;
import org.mule.el.context.AppContext;
import org.mule.el.context.MessageContext;
import org.mule.el.function.RegexExpressionLanguageFuntion;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.PropertyAccessException;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.MimeType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class MVELExpressionLanguageTestCase extends AbstractMuleContextTestCase
{

    protected Variant variant;
    protected MVELExpressionLanguage mvel;
    final String largeExpression = "payload = 'Tom,Fennelly,Male,4,Ireland';StringBuilder sb = new StringBuilder(); fields = payload.split(',\');"
                                   + "if (fields.length > 4) {"
                                   + "    sb.append('  <Contact>\n');"
                                   + "    sb.append('    <FirstName>').append(fields[0]).append('</FirstName>\n');"
                                   + "    sb.append('    <LastName>').append(fields[1]).append('</LastName>\n');"
                                   + "    sb.append('    <Address>').append(fields[2]).append('</Address>\n');"
                                   + "    sb.append('    <TelNum>').append(fields[3]).append('</TelNum>\n');"
                                   + "    sb.append('    <SIN>').append(fields[4]).append('</SIN>\n');"
                                   + "    sb.append('  </Contact>\n');" + "}" + "sb.toString();";

    public MVELExpressionLanguageTestCase(Variant variant, String mvelOptimizer)
    {
        this.variant = variant;
        if (mvelOptimizer != null)
        {
            OptimizerFactory.setDefaultOptimizer(mvelOptimizer);
        }
    }

    @Before
    public void setupMVEL() throws InitialisationException
    {
        mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();
    }

    @Test
    public void testEvaluateString()
    {
        // Literals
        assertEquals("hi", evaluate("'hi'"));
        assertEquals(4, evaluate("2*2"));
        assertEquals("hiho", evaluate("'hi'+'ho'"));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone"));
        assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version"));
        assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name"));
    }

    @Test
    public void testEvaluateStringMapOfStringObject()
    {
        // Literals
        assertEquals("hi", evaluate("'hi'", Collections.<String, Object> emptyMap()));
        assertEquals(4, evaluate("2*2", Collections.<String, Object> emptyMap()));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(),
            evaluate("server.timeZone", Collections.<String, Object> emptyMap()));
        assertEquals(MuleManifest.getProductVersion(),
            evaluate("mule.version", Collections.<String, Object> emptyMap()));
        assertEquals(muleContext.getConfiguration().getId(),
            evaluate("app.name", Collections.<String, Object> emptyMap()));

        // Custom variables (via method param)
        assertEquals(1, evaluate("foo", Collections.<String, Object> singletonMap("foo", 1)));
        assertEquals("bar", evaluate("foo", Collections.<String, Object> singletonMap("foo", "bar")));
    }

    @Test
    public void testEvaluateStringMuleEvent()
    {
        MuleEvent event = createMockEvent();

        // Literals
        assertEquals("hi", evaluate("'hi'", event));
        assertEquals(4, evaluate("2*2", event));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone", event));
        assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version", event));
        assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name", event));

        // Event context
        assertEquals("myFlow", evaluate("flow.name", event));
        assertEquals("foo", evaluate("message.payload", event));

    }

    @Test
    public void testEvaluateStringMuleEventMapOfStringObject()
    {
        MuleEvent event = createMockEvent();

        // Literals
        assertEquals("hi", evaluate("'hi'", event));
        assertEquals(4, evaluate("2*2", event));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone", event));
        assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version", event));
        assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name", event));

        // Event context
        assertEquals("myFlow", evaluate("flow.name", event));
        assertEquals("foo", evaluate("message.payload", event));

        // Custom variables (via method param)
        assertEquals(1, evaluate("foo", Collections.<String, Object> singletonMap("foo", 1)));
        assertEquals("bar", evaluate("foo", Collections.<String, Object> singletonMap("foo", "bar")));
    }

    @Test
    public void testEvaluateStringMuleMessage()
    {
        MuleMessage message = createMockMessage();

        // Literals
        assertEquals("hi", evaluate("'hi'", message));
        assertEquals(4, evaluate("2*2", message));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone", message));
        assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version", message));
        assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name", message));

        // Event context
        assertEquals("foo", evaluate("message.payload", message));
    }

    @Test
    public void testEvaluateStringMuleMessageMapOfStringObject()
    {
        MuleMessage message = createMockMessage();

        // Literals
        assertEquals("hi", evaluate("'hi'", message));
        assertEquals(4, evaluate("2*2", message));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone", message));
        assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version", message));
        assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name", message));

        // Event context
        assertEquals("foo", evaluate("message.payload", message));

        // Custom variables (via method param)
        assertEquals(1, evaluate("foo", Collections.<String, Object> singletonMap("foo", 1)));
        assertEquals("bar", evaluate("foo", Collections.<String, Object> singletonMap("foo", "bar")));
    }

    @Test
    public void testIsValid()
    {
        assertTrue(mvel.isValid("2*2"));
    }

    @Test
    public void testIsValidInvalid()
    {
        assertFalse(mvel.isValid("2*'2"));
    }

    @Test
    public void testValidate()
    {
        validate("2*2");
    }

    @Test(expected = InvalidExpressionException.class)
    public void testValidateInvalid()
    {
        validate("2*'2");
    }

    @Test
    public void regexFunction()
    {
        assertEquals("foo",
            evaluate("regex('TEST(\\\\w+)TEST')", new DefaultMuleMessage("TESTfooTEST", muleContext)));
    }

    @Test
    public void appTakesPrecedenceOverEverything() throws RegistrationException, InitialisationException
    {
        mvel.setAliases(Collections.singletonMap("app", "'other1'"));
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInvocationProperty("app", "otherb");
        muleContext.getRegistry().registerObject("foo", new ExpressionLanguageExtension()
        {
            @Override
            public void configureContext(ExpressionLanguageContext context)
            {
                context.addVariable("app", "otherc");
            }
        });
        mvel.initialise();
        assertEquals(AppContext.class, evaluate("app").getClass());
    }

    @Test
    public void messageTakesPrecedenceOverEverything() throws RegistrationException, InitialisationException
    {
        mvel.setAliases(Collections.singletonMap("message", "'other1'"));
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInvocationProperty("message", "other2");
        muleContext.getRegistry().registerObject("foo", new ExpressionLanguageExtension()
        {
            @Override
            public void configureContext(ExpressionLanguageContext context)
            {
                context.addVariable("message", "other3");
            }
        });
        mvel.initialise();
        assertEquals(MessageContext.class, evaluate("message", message).getClass());
    }

    @Test
    public void extensionTakesPrecedenceOverAutoResolved()
        throws RegistrationException, InitialisationException
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInvocationProperty("foo", "other");
        muleContext.getRegistry().registerObject("key", new ExpressionLanguageExtension()
        {
            @Override
            public void configureContext(ExpressionLanguageContext context)
            {
                context.addVariable("foo", "bar");
            }
        });
        mvel.initialise();
        assertEquals("bar", evaluate("foo", message));
    }

    @Test
    public void aliasTakesPrecedenceOverAutoResolved() throws RegistrationException, InitialisationException
    {
        mvel.setAliases(Collections.singletonMap("foo", "'bar'"));
        muleContext.getRegistry().registerObject("key", new ExpressionLanguageExtension()
        {
            @Override
            public void configureContext(ExpressionLanguageContext context)
            {
                context.addVariable("foo", "other");
            }
        });
        mvel.initialise();
        assertEquals("bar", evaluate("foo"));
    }

    @Test
    public void aliasTakesPrecedenceOverExtension() throws RegistrationException, InitialisationException
    {
        mvel.setAliases(Collections.singletonMap("foo", "'bar'"));
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInvocationProperty("foo", "other");
        mvel.initialise();
        assertEquals("bar", evaluate("foo"));
    }

    @Test
    public void addImport() throws InitialisationException
    {
        mvel.setImports(Collections.<String, Class<?>> singletonMap("loc", Locale.class));
        mvel.initialise();
        assertEquals(Locale.class, evaluate("loc"));
    }

    @Test
    public void addAlias() throws InitialisationException
    {
        mvel.setAliases(Collections.<String, String> singletonMap("appName", "app.name"));
        mvel.initialise();
        assertEquals(muleContext.getConfiguration().getId(), evaluate("appName"));
    }

    @Test
    public void addGlobalFunction() throws InitialisationException
    {
        mvel.addGlobalFunction("hello", new HelloWorldFunction(new ParserContext(mvel.parserConfiguration)));
        mvel.initialise();
        assertEquals("Hello World!", evaluate("hello()"));
    }

    @Test
    public void defaultImports() throws InitialisationException, ClassNotFoundException, IOException
    {
        // java.io.*
        assertEquals(InputStream.class, evaluate(InputStream.class.getSimpleName()));
        assertEquals(FileReader.class, evaluate(FileReader.class.getSimpleName()));
        // java.lang.*
        assertEquals(Object.class, evaluate(Object.class.getSimpleName()));
        assertEquals(System.class, evaluate(System.class.getSimpleName()));
        // java.net.*
        assertEquals(URI.class, evaluate(URI.class.getSimpleName()));
        assertEquals(URL.class, evaluate(URL.class.getSimpleName()));
        // java.util.*
        assertEquals(Collection.class, evaluate(Collection.class.getSimpleName()));
        assertEquals(List.class, evaluate(List.class.getSimpleName()));

        assertEquals(BigDecimal.class, evaluate(BigDecimal.class.getSimpleName()));
        assertEquals(BigInteger.class, evaluate(BigInteger.class.getSimpleName()));
        assertEquals(DataHandler.class, evaluate(DataHandler.class.getSimpleName()));
        assertEquals(MimeType.class, evaluate(MimeType.class.getSimpleName()));
        assertEquals(Pattern.class, evaluate(Pattern.class.getSimpleName()));
        assertEquals(DataType.class, evaluate(DataType.class.getSimpleName()));
        assertEquals(DataTypeFactory.class, evaluate(DataTypeFactory.class.getSimpleName()));

    }

    static class DummyExpressionLanguageExtension implements ExpressionLanguageExtension
    {
        @Override
        public void configureContext(ExpressionLanguageContext context)
        {
            for (int i = 0; i < 20; i++)
            {
                context.declareFunction("dummy-function-" + i, new RegexExpressionLanguageFuntion());
            }
        }
    }

    @Test
    public void testConcurrentCompilation() throws Exception
    {
        final int N = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(N);
        final AtomicInteger errors = new AtomicInteger(0);
        for (int i = 0; i < N; i++)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        start.await();
                        evaluate(largeExpression + new Random().nextInt());
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        errors.incrementAndGet();
                    }
                    finally
                    {
                        end.countDown();
                    }
                }
            }, "thread-eval-" + i).start();
        }
        start.countDown();
        end.await();
        if (errors.get() > 0)
        {
            fail();
        }
    }

    @Test
    public void testConcurrentEvaluation() throws Exception
    {
        final int N = 100;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(N);
        final AtomicInteger errors = new AtomicInteger(0);
        for (int i = 0; i < N; i++)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        start.await();
                        testEvaluateString();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        errors.incrementAndGet();
                    }
                    finally
                    {
                        end.countDown();
                    }
                }
            }, "thread-eval-" + i).start();
        }
        start.countDown();
        end.await();
        if (errors.get() > 0)
        {
            fail();
        }
    }

    @Test
    public void propertyAccessException() throws InitialisationException
    {
        try
        {
            evaluate("doesntExist");
        }
        catch (Exception e)
        {
            assertEquals(ExpressionRuntimeException.class, e.getClass());
            assertEquals(PropertyAccessException.class, e.getCause().getClass());
        }
    }

    @Test
    public void propertyAccessException2() throws InitialisationException
    {
        try
        {
            evaluate("app.doesntExist");
        }
        catch (Exception e)
        {
            assertEquals(ExpressionRuntimeException.class, e.getClass());
            assertEquals(PropertyAccessException.class, e.getCause().getClass());
        }
    }

    protected Object evaluate(String expression)
    {
        if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER))
        {
            return mvel.evaluate("#[" + expression + "]");
        }
        else
        {
            return mvel.evaluate(expression);
        }
    }

    protected Object evaluate(String expression, Map<String, Object> vars)
    {
        if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER))
        {
            return mvel.evaluate("#[" + expression + "]", vars);
        }
        else
        {
            return mvel.evaluate(expression, vars);
        }
    }

    protected Object evaluate(String expression, MuleMessage message)
    {
        if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER))
        {
            return mvel.evaluate("#[" + expression + "]", message);
        }
        else
        {
            return mvel.evaluate(expression, message);
        }
    }

    protected Object evaluate(String expression, MuleEvent event)
    {
        if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER))
        {
            return mvel.evaluate("#[" + expression + "]", event);
        }
        else
        {
            return mvel.evaluate(expression, event);
        }
    }

    protected void validate(String expression)
    {
        if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER))
        {
            mvel.validate("#[" + expression + "]");
        }
        else
        {
            mvel.validate(expression);
        }
    }

    protected MuleEvent createMockEvent()
    {
        MuleEvent event = mock(MuleEvent.class);
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        when(flowConstruct.getName()).thenReturn("myFlow");
        MuleMessage message = createMockMessage();
        Mockito.when(event.getFlowConstruct()).thenReturn(flowConstruct);
        Mockito.when(event.getMessage()).thenReturn(message);
        return event;
    }

    protected MuleMessage createMockMessage()
    {
        MuleMessage message = mock(MuleMessage.class);
        Mockito.when(message.getPayload()).thenReturn("foo");
        return message;
    }

    public static enum Variant
    {
        EXPRESSION_WITH_DELIMITER, EXPRESSION_STRAIGHT_UP
    }

    @Parameters
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {Variant.EXPRESSION_WITH_DELIMITER, OptimizerFactory.SAFE_REFLECTIVE},
            {Variant.EXPRESSION_WITH_DELIMITER, "ASM"}, {Variant.EXPRESSION_WITH_DELIMITER, null},
            {Variant.EXPRESSION_STRAIGHT_UP, OptimizerFactory.SAFE_REFLECTIVE},
            {Variant.EXPRESSION_STRAIGHT_UP, "ASM"}, {Variant.EXPRESSION_STRAIGHT_UP, null},});
    }

    private static class HelloWorldFunction extends Function
    {
        public HelloWorldFunction(ParserContext parserContext)
        {
            super("hello", new char[]{}, 0, 0, 0, 0, 0, parserContext);
        }

        @Override
        public Object call(Object ctx,
                           Object thisValue,
                           org.mule.mvel2.integration.VariableResolverFactory factory,
                           Object[] parms)
        {
            return "Hello World!";
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and
     * subpackages.
     * 
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements())
        {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs)
        {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     * 
     * @param directory The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException
    {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists())
        {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files)
        {
            if (file.getName().endsWith(".class"))
            {
                classes.add(Class.forName(packageName + '.'
                                          + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    @Test
    public void collectionAccessPayloadChangedMULE7506() throws Exception
    {
        MuleEvent event = getTestEvent(new String[]{"1", "2"});
        assertEquals("1", mvel.evaluate("payload[0]", event));
        event.getMessage().setPayload(Collections.singletonList("1"));
        assertEquals("1", mvel.evaluate("payload[0]", event));
    }

}
