/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.some.extension;

import static java.math.BigDecimal.valueOf;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Text;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Some operations
 */
public class SomeOps {

  private final Logger LOGGER = getLogger(SomeOps.class);

  public void someOp(@Connection String conn, @Config ParameterGroupConfig ext) {}

  public ParameterGroupConfig retrieveConfiguration(@Config ParameterGroupConfig config) {
    return config;
  }

  public ZonedDateTime retrieveZonedDateTime(ZonedDateTime zonedDateTime) {
    return zonedDateTime;
  }

  /**
   * An operation to test the ByteArray to InputStream value transformation.
   *
   * @param value the value for the operation to consume
   * @return a byte array representation of <it>value</it>
   */
  @MediaType(ANY)
  public Result<InputStream, Object> inputStreamConsumingOperation(@org.mule.sdk.api.annotation.param.Content TypedValue<InputStream> value) {
    LOGGER.info("A new message is passing through 'inputStreamConsumingOperation': {}", value.getValue());
    return Result.<InputStream, Object>builder().output(value.getValue()).attributes(null).build();
  }

  /**
   * An operation to test an use-case of a ParameterGroup shown as a child-element of the operation xml definition, with a
   * <it>isOneRequired</it> property.
   *
   * @param oneParameterGroup some test operation config with a isOneRequired exclusive-optional configuration
   * @return the config
   */
  public SomeParameterGroupOneRequiredConfig oneRequiredParameterResolverOperationDslTrue(@org.mule.sdk.api.annotation.param.ParameterGroup(
      name = "Awesome Parameter Group", showInDsl = true) SomeParameterGroupOneRequiredConfig oneParameterGroup) {
    return oneParameterGroup;
  }

  /**
   * An operation to test an use-case of a exclusive-optionals <it>isOneRequired</it> ParameterGroup and repeated parameter names.
   */
  public Map<String, String> repeatedParameterName(ComplexParameter pojoParameter, @ParameterGroup(
      name = "Awesome Parameter Group") SomeParameterGroupOneRequiredConfig oneParameterGroup) {
    Map<String, String> values = new HashMap<>();
    values.put("pojoParameter", pojoParameter.getRepeatedNameParameter());
    if (oneParameterGroup.getComplexParameter() != null) {
      values.put("oneParameterGroup", oneParameterGroup.getComplexParameter().getRepeatedNameParameter());
    } else if (oneParameterGroup.getRepeatedNameParameter() != null) {
      values.put("oneParameterGroup", oneParameterGroup.getRepeatedNameParameter());
    }
    return values;
  }

  /**
   * An operation to test an use-case of a exclusive-optionals <it>isOneRequired</it> ParameterGroup with show in Dsl true and
   * repeated parameter names.
   */
  public Map<String, String> repeatedParameterNameDslTrue(ComplexParameter pojoParameter, @ParameterGroup(
      name = "Awesome Parameter Group", showInDsl = true) SomeParameterGroupOneRequiredConfig oneParameterGroup) {
    Map<String, String> values = new HashMap<>();
    values.put("pojoParameter", pojoParameter.getRepeatedNameParameter());
    if (oneParameterGroup.getComplexParameter() != null) {
      values.put("oneParameterGroup", oneParameterGroup.getComplexParameter().getRepeatedNameParameter());
    } else if (oneParameterGroup.getRepeatedNameParameter() != null) {
      values.put("oneParameterGroup", oneParameterGroup.getRepeatedNameParameter());
    }
    return values;
  }

  /**
   * An operation to test an use-case of a exclusive-optionals <it>isOneRequired</it> ParameterGroup where show in Dsl is off.
   */
  public SomeParameterGroupOneRequiredConfig oneRequiredParameterResolverOperation(@ParameterGroup(
      name = "Awesome Parameter Group") SomeParameterGroupOneRequiredConfig oneParameterGroup) {
    return oneParameterGroup;
  }


  /**
   * An operation to test an use-case of a exclusive-optionals <it>isOneRequired</it> ParameterGroup with aliased parameters where
   * show in Dsl is true.
   */
  public SomeAliasedParameterGroupOneRequiredConfig oneRequiredAliasedParameterResolverOperationDslTrue(@ParameterGroup(
      name = "Aliased Parameter Group", showInDsl = true) SomeAliasedParameterGroupOneRequiredConfig oneAliasedParameterGroup) {
    return oneAliasedParameterGroup;
  }

  /**
   * An operation to test an use-case of a exclusive-optionals <it>isOneRequired</it> ParameterGroup with aliased parameters where
   * show in Dsl is off.
   */
  public SomeAliasedParameterGroupOneRequiredConfig oneRequiredAliasedParameterResolverOperation(@ParameterGroup(
      name = "Aliased Parameter Group") SomeAliasedParameterGroupOneRequiredConfig oneAliasedParameterGroup) {
    return oneAliasedParameterGroup;
  }

  @MediaType(value = TEXT_PLAIN)
  public String sayHi(String person) {
    return buildHelloMessage(person);
  }

  @MediaType(value = TEXT_PLAIN)
  public String sayHiContent(@Content String person) {
    return buildHelloMessage(person);
  }

  @MediaType(value = TEXT_PLAIN)
  public String sayHiText(@Text String persona) {
    return buildHelloMessage(persona);
  }

  @MediaType(value = TEXT_PLAIN)
  public String sayHiPojo(PersonPojo personPojo) {
    return buildHelloMessage(personPojo.getName());
  }

  @MediaType(value = TEXT_PLAIN)
  public String sayHiParameterGroup(@ParameterGroup(name = "person-pg") PersonPojo personPojo) {
    return buildHelloMessage(personPojo.getName());
  }

  @MediaType(value = TEXT_PLAIN)
  public String sayHiParameterGroupDsl(@ParameterGroup(name = "person-pg-dsl", showInDsl = true) PersonPojo personPojo) {
    return buildHelloMessage(personPojo.getName());
  }

  public BigDecimal sumBigDecimal(BigDecimal x, BigDecimal y) {
    return x.add(y);
  }

  public BigDecimal sumBigDecimalList(List<BigDecimal> numbers) {
    if (numbers.size() == 0) {
      return valueOf(0);
    }
    return numbers.stream().reduce(BigDecimal::add).get();
  }

  public BigInteger sumBigInteger(BigInteger a, BigInteger b) {
    return a.add(b);
  }

  public BigInteger sumBigIntegerList(List<BigInteger> listNumbers) {
    if (listNumbers.size() == 0) {
      return BigInteger.valueOf(0);
    }
    return listNumbers.stream().reduce(BigInteger::add).get();
  }

  private String buildHelloMessage(String person) {
    return "Hello " + person + "!";
  }
}
