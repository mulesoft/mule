/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import org.mule.extension.validation.api.NumberType;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.runners.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class NumberValidationTestCase extends ValidationTestCase {

  private static final String FLOW_NAME = "validateNumber";

  private final String value;
  private final Number minValue;
  private final Number maxValue;
  private final Number lowerBoundaryViolation;
  private final Number upperBoundaryViolation;
  private final NumberType numberType;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {"long", Long.MAX_VALUE / 2, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, Long.MIN_VALUE, Long.MAX_VALUE, NumberType.LONG},
        {"integer", Integer.MAX_VALUE / 2, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MIN_VALUE, Integer.MAX_VALUE,
            NumberType.INTEGER},
        {"short", new Short("100"), new Integer(Short.MIN_VALUE + 1).shortValue(), new Integer(Short.MAX_VALUE - 1).shortValue(),
            Short.MIN_VALUE, Short.MAX_VALUE, NumberType.SHORT},
        {"double", 10D, 1D, 10D, Double.MIN_VALUE, Double.MAX_VALUE, NumberType.DOUBLE},
        {"float", 10F, 1F, 10F, 0F, 20F, NumberType.DOUBLE}});
  }

  public NumberValidationTestCase(String name, Number value, Number minValue, Number maxValue, Number lowerBoundaryViolation,
                                  Number upperBoundaryViolation, NumberType numberType) {
    this.value = value.toString();
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.lowerBoundaryViolation = lowerBoundaryViolation;
    this.upperBoundaryViolation = upperBoundaryViolation;
    this.numberType = numberType;
  }

  @Override
  protected String getConfigFile() {
    return "number-validations.xml";
  }

  @Test
  public void validateNumber() throws Exception {
    assertValid(configureNumberValidationRunner(flowRunner(FLOW_NAME), value, minValue, maxValue));
    final String invalid = "unparseable";
    assertInvalid(configureNumberValidationRunner(flowRunner(FLOW_NAME), invalid, minValue, maxValue),
                  messages.invalidNumberType(invalid, numberType.name()));

    assertInvalid(configureNumberValidationRunner(flowRunner(FLOW_NAME), upperBoundaryViolation, minValue, maxValue),
                  messages.greaterThan(upperBoundaryViolation, maxValue));
    assertInvalid(configureNumberValidationRunner(flowRunner(FLOW_NAME), lowerBoundaryViolation, minValue, maxValue),
                  messages.lowerThan(lowerBoundaryViolation, minValue));
  }

  private FlowRunner configureNumberValidationRunner(FlowRunner runner, Object value, Object minValue, Object maxValue) {
    return runner.withPayload(value).withFlowVariable("minValue", minValue).withFlowVariable("maxValue", maxValue)
        .withFlowVariable("numberType", numberType);
  }
}
