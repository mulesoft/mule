/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.parameter.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
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
