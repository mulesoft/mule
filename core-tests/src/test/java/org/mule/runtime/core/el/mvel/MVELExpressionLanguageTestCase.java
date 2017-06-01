/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.PropertyAccessException;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguageContext;
import org.mule.runtime.core.api.el.ExpressionLanguageExtension;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.MuleManifest;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.el.context.AppContext;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.core.el.function.RegexExpressionLanguageFuntion;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

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
import java.util.HashMap;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MVELExpressionLanguageTestCase extends AbstractMuleContextTestCase {

  protected Variant variant;
  protected MVELExpressionLanguage mvel;
  private FlowConstruct flowConstruct;

  final String largeExpression =
      "payload = 'Tom,Fennelly,Male,4,Ireland';StringBuilder sb = new StringBuilder(); fields = payload.split(',\');"
          + "if (fields.length > 4) {" + "    sb.append('  <Contact>\n');"
          + "    sb.append('    <FirstName>').append(fields[0]).append('</FirstName>\n');"
          + "    sb.append('    <LastName>').append(fields[1]).append('</LastName>\n');"
          + "    sb.append('    <Address>').append(fields[2]).append('</Address>\n');"
          + "    sb.append('    <TelNum>').append(fields[3]).append('</TelNum>\n');"
          + "    sb.append('    <SIN>').append(fields[4]).append('</SIN>\n');" + "    sb.append('  </Contact>\n');" + "}"
          + "sb.toString();";

  public MVELExpressionLanguageTestCase(Variant variant, String mvelOptimizer) {
    this.variant = variant;
    OptimizerFactory.setDefaultOptimizer(mvelOptimizer);
  }

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Before
  public void setupMVEL() throws InitialisationException {
    mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getName()).thenReturn("myFlow");
  }

  @Test
  public void testEvaluateString() {
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
  public void testEvaluateStringMapOfStringObject() {
    // Literals
    assertEquals("hi", evaluate("'hi'", Collections.<String, Object>emptyMap()));
    assertEquals(4, evaluate("2*2", Collections.<String, Object>emptyMap()));

    // Static context
    assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone", Collections.<String, Object>emptyMap()));
    assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version", Collections.<String, Object>emptyMap()));
    assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name", Collections.<String, Object>emptyMap()));

    // Custom variables (via method param)
    assertEquals(1, evaluate("foo", Collections.<String, Object>singletonMap("foo", 1)));
    assertEquals("bar", evaluate("foo", Collections.<String, Object>singletonMap("foo", "bar")));
  }

  @Test
  public void testEvaluateStringMuleEvent() throws Exception {
    Event event = createMockEvent();

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
  public void testEvaluateMapOfStringObject() throws Exception {
    Event event = createMockEvent();

    // Custom variables (via method param)
    assertEquals(1, evaluate("foo", Collections.<String, Object>singletonMap("foo", 1)));
    assertEquals("bar", evaluate("foo", Collections.<String, Object>singletonMap("foo", "bar")));
  }

  @Test
  public void testEvaluateStringMuleMessage() throws Exception {
    Event event = createMockEvent();

    // Event context
    assertEquals("foo", evaluate("message.payload", event));
  }

  @Test
  public void testEvaluateAttributes() throws Exception {
    Event event = createMockEventWithAttributes();

    // Event context
    assertEquals("number 1", evaluate("attributes.one", event));
    assertEquals("number 2", evaluate("attributes.two", event));
  }

  @Test
  public void testValidate() {
    assertThat(validate("2*2").isSuccess(), is(true));
  }

  @Test
  public void testValidateInvalid() {
    assertThat(validate("2*'2").isSuccess(), is(false));
  }

  @Test
  public void regexFunction() throws Exception {
    final Event testEvent = eventBuilder().message(of("TESTfooTEST")).build();
    assertEquals("foo", evaluate("regex('TEST(\\\\w+)TEST')", testEvent));
  }

  @Test
  public void appTakesPrecedenceOverEverything() throws Exception {
    mvel.setAliases(Collections.singletonMap("app", "'other1'"));
    Event event = eventBuilder().message(of("")).addVariable("app", "otherb").build();
    muleContext.getRegistry().registerObject("foo",
                                             (ExpressionLanguageExtension) context -> context.addVariable("app", "otherc"));
    mvel.initialise();
    assertEquals(AppContext.class, evaluate("app", event).getClass());
  }

  @Test
  public void messageTakesPrecedenceOverEverything() throws Exception {
    mvel.setAliases(Collections.singletonMap("message", "'other1'"));
    Event event = eventBuilder().message(of("")).addVariable("message", "other2").build();
    muleContext.getRegistry().registerObject("foo",
                                             (ExpressionLanguageExtension) context -> context.addVariable("message", "other3"));
    mvel.initialise();
    assertEquals(MessageContext.class, evaluate("message", event).getClass());
  }

  @Test
  public void extensionTakesPrecedenceOverAutoResolved() throws Exception {
    Event event = eventBuilder().message(of("")).addVariable("foo", "other").build();
    muleContext.getRegistry().registerObject("key", (ExpressionLanguageExtension) context -> context.addVariable("foo", "bar"));
    mvel.initialise();
    assertEquals("bar", evaluate("foo", event));
  }

  @Test
  public void aliasTakesPrecedenceOverAutoResolved() throws RegistrationException, InitialisationException {
    mvel.setAliases(Collections.singletonMap("foo", "'bar'"));
    muleContext.getRegistry().registerObject("key", (ExpressionLanguageExtension) context -> context.addVariable("foo", "other"));
    mvel.initialise();
    assertEquals("bar", evaluate("foo"));
  }

  @Test
  public void aliasTakesPrecedenceOverExtension() throws Exception {
    mvel.setAliases(Collections.singletonMap("foo", "'bar'"));
    mvel.initialise();
    assertEquals("bar", evaluate("foo"));
  }

  @Test
  public void addImport() throws InitialisationException {
    mvel.setImports(Collections.<String, Class<?>>singletonMap("loc", Locale.class));
    mvel.initialise();
    assertEquals(Locale.class, evaluate("loc"));
  }

  @Test
  public void addAlias() throws InitialisationException {
    mvel.setAliases(Collections.<String, String>singletonMap("appName", "app.name"));
    mvel.initialise();
    assertEquals(muleContext.getConfiguration().getId(), evaluate("appName"));
  }

  @Test
  public void addGlobalFunction() throws InitialisationException {
    mvel.addGlobalFunction("hello", new HelloWorldFunction(new ParserContext(mvel.parserConfiguration)));
    mvel.initialise();
    assertEquals("Hello World!", evaluate("hello()"));
  }

  @Test
  public void defaultImports() throws InitialisationException, ClassNotFoundException, IOException {
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
    assertEquals(AbstractDataTypeBuilderFactory.class, evaluate(AbstractDataTypeBuilderFactory.class.getSimpleName()));
  }

  static class DummyExpressionLanguageExtension implements ExpressionLanguageExtension {

    @Override
    public void configureContext(ExpressionLanguageContext context) {
      for (int i = 0; i < 20; i++) {
        context.declareFunction("dummy-function-" + i, new RegexExpressionLanguageFuntion());
      }
    }
  }

  @Test
  public void testConcurrentCompilation() throws Exception {
    final int N = 100;
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch end = new CountDownLatch(N);
    final AtomicInteger errors = new AtomicInteger(0);
    for (int i = 0; i < N; i++) {
      new Thread(() -> {
        try {
          start.await();
          evaluate(largeExpression + new Random().nextInt());
        } catch (Exception e) {
          e.printStackTrace();
          errors.incrementAndGet();
        } finally {
          end.countDown();
        }
      }, "thread-eval-" + i).start();
    }
    start.countDown();
    end.await();
    if (errors.get() > 0) {
      fail();
    }
  }

  @Test
  public void testConcurrentEvaluation() throws Exception {
    final int N = 100;
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch end = new CountDownLatch(N);
    final AtomicInteger errors = new AtomicInteger(0);
    for (int i = 0; i < N; i++) {
      new Thread(() -> {
        try {
          start.await();
          testEvaluateString();
        } catch (Exception e) {
          e.printStackTrace();
          errors.incrementAndGet();
        } finally {
          end.countDown();
        }
      }, "thread-eval-" + i).start();
    }
    start.countDown();
    end.await();
    if (errors.get() > 0) {
      fail();
    }
  }

  @Test
  public void propertyAccessException() throws InitialisationException {
    try {
      evaluate("doesntExist");
    } catch (Exception e) {
      assertEquals(ExpressionRuntimeException.class, e.getClass());
      assertThat(e.getCause(), instanceOf(CompileException.class));
    }
  }

  @Test
  public void propertyAccessException2() throws InitialisationException {
    try {
      evaluate("app.doesntExist");
    } catch (Exception e) {
      assertEquals(ExpressionRuntimeException.class, e.getClass());
      assertEquals(PropertyAccessException.class, e.getCause().getClass());
    }
  }

  @Test
  public void expressionExceptionHasMvelCauseMessage() throws InitialisationException {
    String expressionWhichThrowsError = "doesntExist";

    expectedEx.expect(ExpressionRuntimeException.class);
    expectedEx.expectMessage(containsString("Error: unresolvable property or identifier: " + expressionWhichThrowsError));
    expectedEx.expectMessage(containsString("evaluating expression: \"" + expressionWhichThrowsError + "\""));

    evaluate(expressionWhichThrowsError);
  }

  @Test
  public void returnsDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(JSON).charset(UTF_16.name()).build();

    Event event = createMockEvent(TEST_MESSAGE, dataType);

    TypedValue typedValue = evaluateTyped("payload", event);

    assertThat((String) typedValue.getValue(), equalTo(TEST_MESSAGE));
    assertThat(typedValue.getDataType(), like(String.class, JSON, UTF_16));
  }

  protected Object evaluate(String expression) {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluateUntyped("#[mel:" + expression + "]", null, null, null, null);
    } else {
      return mvel.evaluateUntyped(expression, null, null, null, null);
    }
  }

  protected TypedValue evaluateTyped(String expression, Event event) throws Exception {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluate("#[mel:" + expression + "]", event, Event.builder(event), flowConstruct,
                           BindingContext.builder().build());
    } else {
      return mvel.evaluate(expression, event, Event.builder(event), flowConstruct, BindingContext.builder().build());
    }
  }

  protected Object evaluate(String expression, Map<String, Object> vars) {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluateUntyped("#[mel:" + expression + "]", vars);
    } else {
      return mvel.evaluateUntyped(expression, vars);
    }
  }

  protected Object evaluate(String expression, Event event) throws Exception {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluateUntyped("#[mel:" + expression + "]", event, Event.builder(event), flowConstruct, null);
    } else {
      return mvel.evaluateUntyped(expression, event, Event.builder(event), flowConstruct, null);
    }
  }

  protected ValidationResult validate(String expression) {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.validate("#[mel:" + expression + "]");
    } else {
      return mvel.validate(expression);
    }
  }

  protected Event createMockEvent(DataType dataType) {
    return createMockEvent("foo", STRING);
  }

  protected Event createMockEventWithAttributes(DataType dataType) {
    HashMap<String, String> attributes = new HashMap<>();
    attributes.put("one", "number 1");
    attributes.put("two", "number 2");
    return createMockEvent("foo", STRING, attributes, OBJECT);
  }

  protected Event createMockEvent(String payload, DataType dataType) {
    return createMockEvent(payload, dataType, NULL_ATTRIBUTES, OBJECT);
  }

  protected Event createMockEvent(String payload, DataType dataType, Object attributes, DataType attributesDataType) {
    Event event = mock(Event.class);
    InternalMessage message = mock(InternalMessage.class);
    when(message.getPayload()).thenReturn(new TypedValue<Object>(payload, dataType));
    when(message.getAttributes()).thenReturn(new TypedValue<Object>(attributes, attributesDataType));
    when(event.getMessage()).thenReturn(message);
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getError()).thenReturn(empty());
    return event;
  }

  protected Event createMockEvent() {
    return createMockEvent(STRING);
  }

  protected Event createMockEventWithAttributes() {
    return createMockEventWithAttributes(STRING);
  }

  public static enum Variant {
    EXPRESSION_WITH_DELIMITER, EXPRESSION_STRAIGHT_UP
  }

  @Parameters
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{Variant.EXPRESSION_WITH_DELIMITER, OptimizerFactory.SAFE_REFLECTIVE},
        {Variant.EXPRESSION_WITH_DELIMITER, OptimizerFactory.DYNAMIC},
        {Variant.EXPRESSION_STRAIGHT_UP, OptimizerFactory.SAFE_REFLECTIVE},
        {Variant.EXPRESSION_STRAIGHT_UP, OptimizerFactory.DYNAMIC}});
  }

  private static class HelloWorldFunction extends Function {

    public HelloWorldFunction(ParserContext parserContext) {
      super("hello", new char[] {}, 0, 0, 0, 0, 0, parserContext);
    }

    @Override
    public Object call(Object ctx, Object thisValue, org.mule.mvel2.integration.VariableResolverFactory factory, Object[] parms) {
      return "Hello World!";
    }
  }

  /**
   * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
   * 
   * @param packageName The base package
   * @return The classes
   * @throws ClassNotFoundException
   * @throws IOException
   */
  private static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList<Class> classes = new ArrayList<>();
    for (File directory : dirs) {
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
  private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
    List<Class> classes = new ArrayList<>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.getName().endsWith(".class")) {
        classes.add(org.apache.commons.lang.ClassUtils
            .getClass(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
      }
    }
    return classes;
  }

  @Test
  public void collectionAccessPayloadChangedMULE7506() throws Exception {
    Event event = eventBuilder().message(of(new String[] {"1", "2"})).build();
    assertEquals("1", mvel.evaluateUntyped("payload[0]", event, Event.builder(event), flowConstruct, null));
    event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(singletonList("1")).build()).build();
    assertEquals("1", mvel.evaluateUntyped("payload[0]", event, Event.builder(event), flowConstruct, null));
  }

}
