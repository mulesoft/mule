/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.parameter.resolver;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;

import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;

import org.junit.Test;

public class ParameterResolverOnPojoTestCase extends AbstractParameterResolverTestCase {

  @Override
  protected String getConfigFile() {
    return "parameter/parameter-resolver-on-pojo-config.xml";
  }

  @Test
  public void operationWithChildElement() throws Exception {
    DifferedKnockableDoor door = getPayload("operationWithChildElement");

    ParameterResolver<String> someExpression = door.getVictim();
    assertParameterResolver(someExpression, of("#[payload]"), is("this is the payload"));
  }

  @Test
  public void operationWithDynamicReferenceElement() throws Exception {
    DifferedKnockableDoor door = getPayload("operationWithDynamicReferenceElement");

    ParameterResolver<String> someExpression = door.getVictim();
    assertParameterResolver(someExpression, of("#[payload]"), is("this is the payload"));
  }

  @Test
  public void operationWithStaticReferenceElement() throws Exception {
    DifferedKnockableDoor door = getPayload("operationWithStaticReferenceElement");

    ParameterResolver<String> someExpression = door.getVictim();
    assertParameterResolver(someExpression, empty(), is("this is not an expression"));
  }


}
