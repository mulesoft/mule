/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.testmodels.services.TestServiceComponent;

/**
 * {@link org.mule.runtime.config.spring.dsl.api.ObjectFactory} for functional test web-service-component
 *
 * @since 4.0
 */
public class TestFunctionalComponentObjectFactory extends FunctionalComponentObjectFactory {

  @Override
  protected FunctionalTestComponent newComponentInstance() {
    return new TestServiceComponent();
  }
}
