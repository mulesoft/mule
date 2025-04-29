/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.internal.event.NullEventFactory;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ExpressionLanguageTransformationValueResolverTestCase extends AbstractMuleTestCase {

  private static final String STRING_VALUE = "Hello World!";
  private static final ValueResolvingContext NULL_VALUE_RESOLVING_CONTEXT =
      ValueResolvingContext.builder(NullEventFactory.getNullEvent()).build();
  private static final String STRING_VALUE_REPRESENTING_NUMBER = "10";
  private static final Integer NUMBER = valueOf(STRING_VALUE_REPRESENTING_NUMBER);
  private static final String POJO_DESCRIPTION = "description!!!";
  private static final String POJO_DESCRIPTION_NAME = "description";
  private static final String POJO_DESCRIPTION_SCORE_NAME = "descriptionScore";
  private static final Integer POJO_DESCRIPTION_SCORE = 500;
  private static final Map<String, Object> CUSTOM_POJO_MAP_REPRESENTATION = new HashMap<>() {

    {
      put(POJO_DESCRIPTION_NAME, POJO_DESCRIPTION);
      put(POJO_DESCRIPTION_SCORE_NAME, String.valueOf(POJO_DESCRIPTION_SCORE));
    }
  };
  private static final CustomPojo CUSTOM_POJO = new CustomPojo(POJO_DESCRIPTION, POJO_DESCRIPTION_SCORE);
  private static final String CUSTOM_POJO_JSON =
      format("{ \"%s\" : \"%s\" , \"%s\": %s }", POJO_DESCRIPTION_NAME, POJO_DESCRIPTION, POJO_DESCRIPTION_SCORE_NAME,
             POJO_DESCRIPTION_SCORE);

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  private ExtendedExpressionManager expressionManager;

  @Before
  public void setUp() throws Exception {
    expressionManager = dw.getExpressionManager();
  }

  @Test
  public void noTransformationNeeded() throws Exception {
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(STRING_VALUE), String.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(STRING_VALUE));
  }

  @Test
  public void noTransformationNeededForTypedValueValue() throws Exception {
    TypedValue<String> typedValue = new TypedValue(STRING_VALUE, DataType.STRING);
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(typedValue),
                                                          String.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(typedValue));
  }

  @Test
  public void transformationNeededForSimpleType() throws Exception {
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(STRING_VALUE_REPRESENTING_NUMBER),
                                                          Integer.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(NUMBER));
  }

  @Test
  public void transformationNeededForSimpleTypeInTypedValue() throws Exception {
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(new TypedValue(STRING_VALUE_REPRESENTING_NUMBER,
                                                                                                 DataType.STRING)),
                                                          Integer.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(NUMBER));
  }

  @Test
  public void transformationNeededForComplexValueInTypedValueValue() throws Exception {
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(new TypedValue(CUSTOM_POJO_JSON,
                                                                                                 DataType.JSON_STRING)),
                                                          CustomPojo.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(CUSTOM_POJO));
  }

  @Test
  public void transformationNeededForComplexValue() throws Exception {
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(CUSTOM_POJO_MAP_REPRESENTATION),
                                                          CustomPojo.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(CUSTOM_POJO));
  }

  @Test
  public void noTransformationNeededForComplexValue() throws Exception {
    ExpressionLanguageTransformationValueResolver valueResolver =
        new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(CUSTOM_POJO),
                                                          CustomPojo.class, expressionManager);
    assertThat(valueResolver.resolve(NULL_VALUE_RESOLVING_CONTEXT), equalTo(CUSTOM_POJO));
  }

  public static class CustomPojo {

    private String description;
    private Integer descriptionScore;

    public CustomPojo(String description, Integer descriptionScore) {
      this.description = description;
      this.descriptionScore = descriptionScore;
    }

    public CustomPojo() {}

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Integer getDescriptionScore() {
      return descriptionScore;
    }

    public void setDescriptionScore(Integer descriptionScore) {
      this.descriptionScore = descriptionScore;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      CustomPojo that = (CustomPojo) o;

      if (description != null ? !description.equals(that.description) : that.description != null)
        return false;
      return descriptionScore != null ? descriptionScore.equals(that.descriptionScore) : that.descriptionScore == null;
    }

    @Override
    public int hashCode() {
      int result = description != null ? description.hashCode() : 0;
      result = 31 * result + (descriptionScore != null ? descriptionScore.hashCode() : 0);
      return result;
    }
  }

}
