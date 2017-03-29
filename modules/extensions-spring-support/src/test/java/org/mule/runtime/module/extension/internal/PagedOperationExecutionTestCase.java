/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.INVOLVED_PEOPLE;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.streaming.objects.CursorIterator;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.PersonalInfo;

import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

public class PagedOperationExecutionTestCase extends ExtensionFunctionalTestCase {

  private static final String SAUL_NEW_NUMBER = "123-12-3";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ConnectionManager connectionManager;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {HeisenbergExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "heisenberg-paged-operation-config.xml";
  }

  @Test
  public void basicPagedOperation() throws Exception {
    CursorIterator<PersonalInfo> streamingIterator = getCursor("getPersonalInfo");
    assertThat(streamingIterator.size(), is(11));
    while (streamingIterator.hasNext()) {
      assertThat((streamingIterator.next()), isIn(INVOLVED_PEOPLE));
    }
  }

  @Test
  public void emptyPagedOperation() throws Exception {
    CursorIterator iterator = getCursor("emptyPagedOperation");
    assertThat(iterator.hasNext(), is(false));
    assertThat(iterator.size(), is(0));
  }

  @Test
  public void pagedOperationException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    getCursor("failingPagedOperation").next();
  }

  @Test
  public void pagedOperationUsingConnection() throws Exception {
    Iterator iterator = getCursor("pagedOperationUsingConnection");
    assertThat(iterator.next().toString(), containsString(SAUL_NEW_NUMBER));
  }

  private <T> CursorIterator<T> getCursor(String flowName) throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).keepStreamsOpen().run().getMessage().getPayload()
            .getValue();

    return provider.openCursor();
  }
}
