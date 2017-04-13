/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDNECY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.ObjectFactoryStory.OBJECT_FACTORY_INECTION_AND_LIFECYCLE;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.test.config.spring.parsers.beans.TestObject;
import org.mule.test.config.spring.parsers.beans.TestObjectFactory;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(LIFECYCLE_AND_DEPENDNECY_INJECTION)
@Stories(OBJECT_FACTORY_INECTION_AND_LIFECYCLE)
public class ObjectFactoryTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/object-factory-config.xml";
  }

  @Test
  public void validateInjectionAndLifecycleOverObjectFactoryAndTheObjectCreatedByIt() throws RegistrationException {
    TestObject testObject = muleContext.getRegistry().lookupObject(TestObject.class);
    assertThat(testObject, notNullValue());

    TestObjectFactory objectFactory = testObject.getObjectFactory();
    assertThat(objectFactory.isInjectionDoneBeforeGetObject(), is(true));
    assertThat(objectFactory.getLifecycleActions().isEmpty(), is(true));

    assertThat(testObject.getLockFactory(), nullValue());

    muleContext.dispose();

    assertThat(testObject.getLifecycleActions(), contains("initialise", "start", "stop", "dispose"));
    assertThat(objectFactory.getLifecycleActions().isEmpty(), is(true));
  }
}
