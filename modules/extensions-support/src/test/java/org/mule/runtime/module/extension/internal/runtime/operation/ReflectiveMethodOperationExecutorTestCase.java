/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.heisenberg.extension.model.HealthStatus.DEAD;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.DefaultExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.LifecycleAwareConfigurationInstance;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ReflectiveMethodOperationExecutorTestCase extends AbstractMuleTestCase {

  private static final String CONFIG_NAME = "config";
  private static final DataType DATA_TYPE = STRING;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Event muleEvent;

  @Mock
  private ResolverSetResult parameters;

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private ConfigurationModel configurationModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private CursorProviderFactory cursorProviderFactory;

  @Mock
  private StreamingManager streamingManager;

  @Mock
  private ComponentLocation location;

  @Mock
  private ConfigurationState configurationState;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  private ReflectiveMethodOperationExecutor executor;
  private ConfigurationInstance configurationInstance;
  private ExecutionContextAdapter operationContext;
  private HeisenbergExtension config;
  private HeisenbergOperations operations;
  private PrimitiveTypesTestOperations primitiveTypesTestOperations = new PrimitiveTypesTestOperations();


  @Before
  public void init() throws Exception {
    initHeisenberg();
    configurationInstance =
        new LifecycleAwareConfigurationInstance(CONFIG_NAME,
                                                configurationModel,
                                                config,
                                                event -> configurationState,
                                                emptyList(),
                                                empty());

    when(muleEvent.getMessage().getPayload()).thenReturn(new TypedValue<>(null, DATA_TYPE));
    when(operationModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(empty());
    operationContext = new DefaultExecutionContext(extensionModel, of(configurationInstance), parameters.asMap(), operationModel,
                                                   muleEvent, cursorProviderFactory, streamingManager, location,
                                                   muleContext);
    operationContext = spy(operationContext);
  }

  @Test
  public void operationWithReturnValueAndWithoutParameters() throws Exception {
    Method method = ClassUtils.getMethod(HeisenbergOperations.class, "sayMyName", new Class<?>[] {HeisenbergExtension.class});
    executor = new ReflectiveMethodOperationExecutor(operationModel, method, operations);
    executor.setMuleContext(muleContext);
    assertResult(execute(), HEISENBERG);
  }

  @Test
  public void exceptionIsPropagated() throws Exception {
    final RuntimeException exception = new RuntimeException();
    operations = mock(HeisenbergOperations.class);
    when(operations.sayMyName(any(HeisenbergExtension.class))).thenThrow(exception);

    try {
      operationWithReturnValueAndWithoutParameters();
      fail("was expecting an exception");
    } catch (Exception e) {
      assertThat(e, is(sameInstance(exception)));
    }
  }

  @Test
  public void voidOperationWithoutParameters() throws Exception {
    Method method = ClassUtils.getMethod(HeisenbergOperations.class, "die", new Class<?>[] {HeisenbergExtension.class});
    executor = new ReflectiveMethodOperationExecutor(operationModel, method, operations);
    executor.setMuleContext(muleContext);
    assertThat(execute(), is(nullValue()));
    assertThat(config.getEndingHealth(), is(DEAD));
  }

  @Test
  public void withArgumentsAndReturnValue() throws Exception {
    Map<String, Object> parametersMap = new HashMap<>();
    parametersMap.put("index", 0);
    when(parameters.asMap()).thenReturn(parametersMap);
    init();

    Method method =
        ClassUtils.getMethod(HeisenbergOperations.class, "getEnemy", new Class<?>[] {HeisenbergExtension.class, int.class});
    executor = new ReflectiveMethodOperationExecutor(operationModel, method, operations);
    executor.setMuleContext(muleContext);

    assertResult(((Result) execute()).getOutput(), "Hank");
  }

  @Test
  public void voidWithArguments() throws Exception {
    Method method = ClassUtils.getMethod(HeisenbergOperations.class, "die", new Class<?>[] {HeisenbergExtension.class});
    executor = new ReflectiveMethodOperationExecutor(operationModel, method, operations);
    executor.setMuleContext(muleContext);
    assertThat(execute(), is(nullValue()));
  }

  @Test
  public void withPrimitiveTypeArgumentsWithoutValue() throws Exception {
    Object[][] primitiveOperations = {{"charOperation", char.class}, {"byteOperation", byte.class},
        {"shortOperation", short.class}, {"intOperation", int.class}, {"longOperation", long.class},
        {"floatOperation", float.class}, {"doubleOperation", double.class}, {"booleanOperation", boolean.class}};
    for (Object[] primitiveOperation : primitiveOperations) {
      Method method = ClassUtils.getMethod(PrimitiveTypesTestOperations.class, (String) primitiveOperation[0],
                                           new Class<?>[] {(Class<?>) primitiveOperation[1]});
      executor = new ReflectiveMethodOperationExecutor(operationModel, method, primitiveTypesTestOperations);
      executor.setMuleContext(muleContext);
      execute();
    }
  }

  @Test
  public void withAllPrimitiveTypeArgumentsWithoutValue() throws Exception {
    Class<?>[] parameterTypes =
        {char.class, byte.class, short.class, int.class, long.class, float.class, double.class, boolean.class};
    Method method = ClassUtils.getMethod(PrimitiveTypesTestOperations.class, "allCombined", parameterTypes);
    executor = new ReflectiveMethodOperationExecutor(operationModel, method, primitiveTypesTestOperations);
    executor.setMuleContext(muleContext);
    execute();
  }

  private void initHeisenberg() {
    config = new HeisenbergExtension();
    config.getPersonalInfo().setName(HEISENBERG);
    config.setEnemies(Arrays.asList("Hank"));
    operations = new HeisenbergOperations();
  }

  private void assertResult(Object value, Object expected) throws Exception {
    assertThat(value, is(expected));
  }

  private Object execute() {
    return from(executor.execute(operationContext)).block();
  }

  public static class PrimitiveTypesTestOperations {

    private char charValue;
    private byte byteValue;
    private short shortValue;
    private int intValue;
    private long longValue;
    private float floatValue;
    private double doubleValue;
    private boolean booleanValue;

    public void charOperation(@org.mule.runtime.extension.api.annotation.param.Optional char value) {
      assertThat(value, is(charValue));
    }

    public void byteOperation(@org.mule.runtime.extension.api.annotation.param.Optional byte value) {
      assertThat(value, is(byteValue));
    }

    public void shortOperation(@org.mule.runtime.extension.api.annotation.param.Optional short value) {
      assertThat(value, is(shortValue));
    }

    public void intOperation(@org.mule.runtime.extension.api.annotation.param.Optional int value) {
      assertThat(value, is(intValue));
    }

    public void longOperation(@org.mule.runtime.extension.api.annotation.param.Optional long value) {
      assertThat(value, is(longValue));
    }

    public void floatOperation(@org.mule.runtime.extension.api.annotation.param.Optional float value) {
      assertThat(value, is(floatValue));
    }

    public void doubleOperation(@org.mule.runtime.extension.api.annotation.param.Optional double value) {
      assertThat(value, is(doubleValue));
    }

    public void booleanOperation(@org.mule.runtime.extension.api.annotation.param.Optional boolean value) {
      assertThat(value, is(booleanValue));
    }

    public void allCombined(@org.mule.runtime.extension.api.annotation.param.Optional char charValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional byte byteValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional short shortValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional int intValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional long longValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional float floatValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional double doubleValue,
                            @org.mule.runtime.extension.api.annotation.param.Optional boolean booleanValue) {
      assertThat(charValue, is(this.charValue));
      assertThat(byteValue, is(this.byteValue));
      assertThat(shortValue, is(this.shortValue));
      assertThat(intValue, is(this.intValue));
      assertThat(longValue, is(this.longValue));
      assertThat(floatValue, is(this.floatValue));
      assertThat(doubleValue, is(this.doubleValue));
      assertThat(booleanValue, is(this.booleanValue));
    }
  }
}
