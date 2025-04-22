/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Issue;

@SmallTest
public class NullSafeValueResolverWrapperTestCase extends AbstractMuleTestCase {

  // Creating new rule with recommended Strictness setting
  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS, lenient = true)
  private CoreEvent event;

  @Mock
  private ObjectTypeParametersResolver objectTypeParametersResolver;

  @Mock
  private TransformationService transformationService;

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  private ExtendedExpressionManager expressionManager;

  private final ReflectionCache reflectionCache = new ReflectionCache();

  @Before
  public void setUp() {
    expressionManager = dw.getExpressionManager();

    when(event.getError()).thenReturn(empty());
    when(event.getAuthentication()).thenReturn(empty());
    Message msg = of(null);
    when(event.getMessage()).thenReturn(msg);
    when(event.asBindingContext()).thenReturn(getTargetBindingContext(msg));
    when(event.getItemSequenceInfo()).thenReturn(empty());
  }

  @Test
  public void testMapType() throws Exception {
    assertExpected(new StaticValueResolver<>(null), toMetadataType(HashMap.class), false, new HashMap<>());
  }

  @Test
  public void testPojoType() throws Exception {
    assertExpected(new StaticValueResolver<>(null), toMetadataType(DynamicPojo.class), true, new DynamicPojo(5));

    verify(event, times(1)).asBindingContext();
  }

  @Test
  @Issue("W-14954976")
  public void testPojoTypeWithLiteralExpression() throws Exception {
    assertExpected(new StaticValueResolver<>(null),
                   toMetadataType(DynamicPojoWithLiteralParam.class),
                   false,
                   new DynamicPojoWithLiteralParam(new ImmutableLiteral<>("#[5]", Integer.class)));

    verify(event, times(1)).asBindingContext();
  }

  @Test
  public void testPojoWithStaticDefaultValue() throws Exception {
    when(transformationService.transform(anyString(), any(), any()))
        .then(inv -> Boolean.valueOf(inv.getArgument(0, String.class)));

    assertExpected(new StaticValueResolver<>(null), toMetadataType(NonDynamicPojo.class), false, new NonDynamicPojo(false));
  }

  @Test
  public void testPojoWithMap() throws Exception {
    DynamicPojoWithMap pojo = new DynamicPojoWithMap();
    pojo.setMap(new HashMap<>());
    assertExpected(new StaticValueResolver<>(null), toMetadataType(DynamicPojoWithMap.class), false, pojo);
  }

  @Test
  public void testNullSafeSdkAndLegacyAnnotation() throws Exception {
    PojoUsingSdkApiAndLegacyApi pojo = new PojoUsingSdkApiAndLegacyApi();
    pojo.setParameters(new ASimplePojo(), new ASimplePojo());
    assertExpected(new StaticValueResolver<>(null), toMetadataType(PojoUsingSdkApiAndLegacyApi.class), false, pojo);
  }

  private void assertExpected(ValueResolver valueResolver, MetadataType type, boolean isDynamic, Object expected)
      throws Exception {
    ValueResolver resolver = NullSafeValueResolverWrapper.of(valueResolver, type, reflectionCache,
                                                             transformationService,
                                                             expressionManager,
                                                             mock(MuleContext.class),
                                                             mock(Injector.class),
                                                             objectTypeParametersResolver);
    ValueResolvingContext ctx = ValueResolvingContext.builder(event)
        .withExpressionManager(expressionManager)
        .build();
    assertThat(resolver.isDynamic(), is(isDynamic));
    assertThat(resolver.resolve(ctx), is(expected));
  }

  public static class DynamicPojo {

    public DynamicPojo() {}

    public DynamicPojo(int time) {
      this.time = time;
    }

    @Parameter
    @Optional(defaultValue = "#[5]")
    private int time;

    public int getTime() {
      return time;
    }

    public void setTime(int time) {
      this.time = time;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof DynamicPojo that) {
        return time == that.time;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(time);
    }
  }


  public static class DynamicPojoWithLiteralParam {

    public DynamicPojoWithLiteralParam() {}

    public DynamicPojoWithLiteralParam(Literal<Integer> time) {
      this.time = time;
    }

    @Parameter
    @Optional(defaultValue = "#[5]")
    private Literal<Integer> time;

    public Literal<Integer> getTime() {
      return time;
    }

    public void setTime(Literal<Integer> time) {
      this.time = time;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof DynamicPojoWithLiteralParam that) {
        return time.getLiteralValue().equals(that.time.getLiteralValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(time.getLiteralValue());
    }
  }


  public static class DynamicPojoWithMap {

    public DynamicPojoWithMap() {}

    @Parameter
    @Optional
    @NullSafe
    private Map<String, String> map;

    public Map<String, String> getMap() {
      return map;
    }

    public void setMap(Map<String, String> map) {
      this.map = map;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof DynamicPojoWithMap that) {
        return Objects.equals(map, that.map);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(map);
    }
  }


  public static class NonDynamicPojo {

    public NonDynamicPojo() {}

    public NonDynamicPojo(Boolean staticDefaultValue) {
      this.staticDefaultValue = staticDefaultValue;
    }

    @Parameter
    @Optional(defaultValue = "false")
    private Boolean staticDefaultValue;

    public Boolean getStaticDefaultValue() {
      return staticDefaultValue;
    }

    public void setStaticDefaultValue(Boolean staticDefaultValue) {
      this.staticDefaultValue = staticDefaultValue;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof NonDynamicPojo that) {
        return Objects.equals(staticDefaultValue, that.staticDefaultValue);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(staticDefaultValue);
    }
  }

  public static class PojoUsingSdkApiAndLegacyApi {

    public PojoUsingSdkApiAndLegacyApi() {}

    @Parameter
    @Optional
    @NullSafe
    private ASimplePojo parameter1;

    @Parameter
    @Optional
    @org.mule.sdk.api.annotation.param.NullSafe
    private ASimplePojo parameter2;

    public void setParameters(ASimplePojo parameter1, ASimplePojo parameter2) {
      this.parameter1 = parameter1;
      this.parameter2 = parameter2;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof PojoUsingSdkApiAndLegacyApi that) {
        return Objects.equals(parameter1, that.parameter1) && Objects.equals(parameter2, that.parameter2);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(parameter1, parameter2);
    }
  }

  public static class ASimplePojo {

    public String parameter = "parameter";

    public ASimplePojo() {}

    @Override
    public boolean equals(Object o) {
      if (o instanceof ASimplePojo that) {
        return Objects.equals(this.parameter, that.parameter);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(parameter);
    }
  }
}
