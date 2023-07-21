/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.parameter.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.sdk.api.runtime.parameter.ParameterResolver;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.hamcrest.Matcher;

public abstract class AbstractParameterResolverTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  protected <T> T getPayload(String flowName) throws Exception {
    return (T) flowRunner(flowName).run().getMessage().getPayload().getValue();
  }

  protected void assertParameterResolver(ParameterResolver resolver, java.util.Optional<String> expression, Matcher value) {
    assertThat(resolver.getExpression(), is(expression));
    assertThat(resolver.resolve(), value);
  }
}
