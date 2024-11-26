/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;

/**
 * Just validate that the deserialization of an event context doesn't fail. After obtaining it, it shouldn't be used, as its
 * relevant state is transient and not valid after deserialization.
 * <p>
 * The serialization of an event context is just a design oversight that cannot be fixed at the moment because it would break
 * compatibility with already existing serialized state.
 */
@Feature(EVENT_CONTEXT)
@Issue("W-16927173")
public class DefaultEventContextSerializationTestCase extends AbstractMuleTestCase {

  // Use this to regenerate serialized files
  // @Before
  public void generateFiles() throws FileNotFoundException, IOException {
    BaseEventContext context = (BaseEventContext) create(mock(FlowConstruct.class), TEST_CONNECTOR_LOCATION);
    try (ObjectOutputStream oos =
        new ObjectOutputStream(new FileOutputStream("src/test/resources/event/context_4.6.ser"))) {
      oos.writeObject(context);
    }

    BaseEventContext contextWithResult = (BaseEventContext) create(mock(FlowConstruct.class), TEST_CONNECTOR_LOCATION);
    contextWithResult.success(CoreEvent.nullEvent());
    try (ObjectOutputStream oos =
        new ObjectOutputStream(new FileOutputStream("src/test/resources/event/context_with_result_4.6.ser"))) {
      oos.writeObject(contextWithResult);
    }
  }

  @Test
  public void deserializeEventContextFrom46() throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(this.getClass().getResourceAsStream("/event/context_4.6.ser"))) {
      final BaseEventContext eventCtx = (BaseEventContext) ois.readObject();

      assertThat(eventCtx, not(nullValue()));
    }
  }

  @Test
  public void deserializeEventContextWithResultFrom46() throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois =
        new ObjectInputStream(this.getClass().getResourceAsStream("/event/context_with_result_4.6.ser"))) {
      final BaseEventContext eventCtx = (BaseEventContext) ois.readObject();

      assertThat(eventCtx, not(nullValue()));
    }
  }

}
