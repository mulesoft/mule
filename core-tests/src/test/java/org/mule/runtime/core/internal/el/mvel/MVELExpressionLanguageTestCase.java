/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.mvel2.optimizers.OptimizerFactory.DYNAMIC;
import static org.mule.mvel2.optimizers.OptimizerFactory.SAFE_REFLECTIVE;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguageTestCase.Variant.EXPRESSION_STRAIGHT_UP;
import static org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguageTestCase.Variant.EXPRESSION_WITH_DELIMITER;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.PropertyAccessException;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.config.MuleManifest;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.el.context.MessageContext;
import org.mule.runtime.core.internal.el.mvel.function.RegexExpressionLanguageFuntion;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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

    flowConstruct = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    when(flowConstruct.getName()).thenReturn("myFlow");
    final DefaultComponentLocation location = fromSingleComponent("myFlow");
    when(((Component) flowConstruct).getAnnotation(LOCATION_KEY)).thenReturn(location);
    when(((Component) flowConstruct).getLocation()).thenReturn(location);
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
    assertEquals("hi", evaluate("'hi'", emptyMap()));
    assertEquals(4, evaluate("2*2", emptyMap()));

    // Static context
    assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone", emptyMap()));
    assertEquals(MuleManifest.getProductVersion(), evaluate("mule.version", emptyMap()));
    assertEquals(muleContext.getConfiguration().getId(), evaluate("app.name", emptyMap()));

    // Custom variables (via method param)
    assertEquals(1, evaluate("foo", singletonMap("foo", 1)));
    assertEquals("bar", evaluate("foo", singletonMap("foo", "bar")));
  }

  @Test
  public void testEvaluateStringMuleEvent() throws Exception {
    PrivilegedEvent event = createEvent();

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
    PrivilegedEvent event = createEvent();

    // Custom variables (via method param)
    assertEquals(1, evaluate("foo", singletonMap("foo", 1)));
    assertEquals("bar", evaluate("foo", singletonMap("foo", "bar")));
  }

  @Test
  public void testEvaluateStringMuleMessage() throws Exception {
    PrivilegedEvent event = createEvent();

    // Event context
    assertEquals("foo", evaluate("message.payload", event));
  }

  @Test
  public void testEvaluateAttributes() throws Exception {
    PrivilegedEvent event = createEventWithAttributes();

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
    final PrivilegedEvent testEvent = this.<PrivilegedEvent.Builder>getEventBuilder().message(of("TESTfooTEST")).build();
    assertEquals("foo", evaluate("regex('TEST(\\\\w+)TEST')", testEvent));
  }

  @Test
  public void appTakesPrecedenceOverEverything() throws Exception {
    mvel.setAliases(singletonMap("app", "'other1'"));
    PrivilegedEvent event = this.<PrivilegedEvent.Builder>getEventBuilder().message(of("")).addVariable("app", "otherb").build();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject("foo",
                                                                           (ExpressionLanguageExtension) context -> context
                                                                               .addVariable("app", "otherc"));
    mvel.initialise();
    assertEquals(MVELArtifactContext.class, evaluate("app", event).getClass());
  }

  @Test
  public void messageTakesPrecedenceOverEverything() throws Exception {
    mvel.setAliases(singletonMap("message", "'other1'"));
    PrivilegedEvent event =
        this.<PrivilegedEvent.Builder>getEventBuilder().message(of("")).addVariable("message", "other2").build();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject("foo",
                                                                           (ExpressionLanguageExtension) context -> context
                                                                               .addVariable("message", "other3"));
    mvel.initialise();
    assertEquals(MessageContext.class, evaluate("message", event).getClass());
  }

  @Test
  public void extensionTakesPrecedenceOverAutoResolved() throws Exception {
    PrivilegedEvent event = this.<PrivilegedEvent.Builder>getEventBuilder().message(of("")).addVariable("foo", "other").build();
    ((MuleContextWithRegistries) muleContext).getRegistry()
        .registerObject("key", (ExpressionLanguageExtension) context -> context.addVariable("foo", "bar"));
    mvel.initialise();
    assertEquals("bar", evaluate("foo", event));
  }

  @Test
  public void aliasTakesPrecedenceOverAutoResolved() throws RegistrationException, InitialisationException {
    mvel.setAliases(singletonMap("foo", "'bar'"));
    ((MuleContextWithRegistries) muleContext).getRegistry()
        .registerObject("key", (ExpressionLanguageExtension) context -> context.addVariable("foo", "other"));
    mvel.initialise();
    assertEquals("bar", evaluate("foo"));
  }

  @Test
  public void aliasTakesPrecedenceOverExtension() throws Exception {
    mvel.setAliases(singletonMap("foo", "'bar'"));
    mvel.initialise();
    assertEquals("bar", evaluate("foo"));
  }

  @Test
  public void addImport() throws InitialisationException {
    mvel.setImports(singletonMap("loc", Locale.class));
    mvel.initialise();
    assertEquals(Locale.class, evaluate("loc"));
  }

  @Test
  public void addAlias() throws InitialisationException {
    mvel.setAliases(singletonMap("appName", "app.name"));
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

    CoreEvent event = createEvent(TEST_MESSAGE, dataType);

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

  protected TypedValue evaluateTyped(String expression, CoreEvent event) throws Exception {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluate("#[mel:" + expression + "]", event, CoreEvent.builder(event),
                           ((Component) flowConstruct).getLocation(),
                           BindingContext.builder().build());
    } else {
      return mvel.evaluate(expression, event, CoreEvent.builder(event), ((Component) flowConstruct).getLocation(),
                           BindingContext.builder().build());
    }
  }

  protected Object evaluate(String expression, Map<String, Object> vars) {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluateUntyped("#[mel:" + expression + "]", vars);
    } else {
      return mvel.evaluateUntyped(expression, vars);
    }
  }

  protected Object evaluate(String expression, PrivilegedEvent event) throws Exception {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.evaluateUntyped("#[mel:" + expression + "]", event, PrivilegedEvent.builder(event),
                                  ((Component) flowConstruct).getLocation(), null);
    } else {
      return mvel.evaluateUntyped(expression, event, PrivilegedEvent.builder(event),
                                  ((Component) flowConstruct).getLocation(), null);
    }
  }

  protected ValidationResult validate(String expression) {
    if (variant.equals(Variant.EXPRESSION_WITH_DELIMITER)) {
      return mvel.validate("#[mel:" + expression + "]");
    } else {
      return mvel.validate(expression);
    }
  }

  protected PrivilegedEvent createEvent(DataType dataType) {
    return createEvent("foo", STRING);
  }

  protected PrivilegedEvent createEventWithAttributes(DataType dataType) {
    HashMap<String, String> attributes = new HashMap<>();
    attributes.put("one", "number 1");
    attributes.put("two", "number 2");
    return createEvent("foo", STRING, attributes, OBJECT);
  }

  protected PrivilegedEvent createEvent(String payload, DataType dataType) {
    return createEvent(payload, dataType, null, OBJECT);
  }

  protected PrivilegedEvent createEvent(String payload, DataType dataType, Object attributes, DataType attributesDataType) {
    InternalMessage message = mock(InternalMessage.class);
    when(message.getPayload()).thenReturn(new TypedValue<>(payload, dataType));
    when(message.getAttributes()).thenReturn(new TypedValue<>(attributes, attributesDataType));

    try {
      return this.<PrivilegedEvent.Builder>getEventBuilder().message(message).build();
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  protected PrivilegedEvent createEvent() {
    return createEvent(STRING);
  }

  protected PrivilegedEvent createEventWithAttributes() {
    return createEventWithAttributes(STRING);
  }

  public enum Variant {
    EXPRESSION_WITH_DELIMITER, EXPRESSION_STRAIGHT_UP
  }

  @Parameters(name = "{0}, {1}")
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {EXPRESSION_WITH_DELIMITER, SAFE_REFLECTIVE},
        {EXPRESSION_WITH_DELIMITER, DYNAMIC},
        {EXPRESSION_STRAIGHT_UP, SAFE_REFLECTIVE},
        {EXPRESSION_STRAIGHT_UP, DYNAMIC}
    });
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

  @Test
  public void collectionAccessPayloadChangedMULE7506() throws Exception {
    PrivilegedEvent event = this.<PrivilegedEvent.Builder>getEventBuilder().message(of(new String[] {"1", "2"})).build();
    assertEquals("1", mvel.evaluateUntyped("payload[0]", event, PrivilegedEvent.builder(event),
                                           ((Component) flowConstruct).getLocation(), null));
    event = PrivilegedEvent.builder(event).message(InternalMessage.builder(event.getMessage()).value(singletonList("1")).build())
        .build();
    assertEquals("1", mvel.evaluateUntyped("payload[0]", event, PrivilegedEvent.builder(event),
                                           ((Component) flowConstruct).getLocation(), null));
  }

}
