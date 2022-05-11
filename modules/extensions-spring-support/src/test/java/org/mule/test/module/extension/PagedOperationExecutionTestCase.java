/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.INVOLVED_PEOPLE;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.closeEmptyOperationCalls;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.closePagingProviderCalls;
import static org.mule.test.heisenberg.extension.MoneyLaunderingOperation.getPageCalls;

import java.util.Iterator;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.runner.RunnerDelegateTo;

@RunnerDelegateTo(Parameterized.class)
public class PagedOperationExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String SAUL_NEW_NUMBER = "123-12-3";

  @Parameters
  public static List<Object[]> data() {
    return asList(new Object[][] {{"true"}, {"false"}});
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public SystemProperty withStatistics;

  @Mock
  private ConnectionManager connectionManager;

  public PagedOperationExecutionTestCase(String enableStatistics) {
    this.withStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, enableStatistics);
  }

  @Override
  protected String getConfigFile() {
    return "heisenberg-paged-operation-config.xml";
  }

  @Test
  public void basicPagedOperation() throws Exception {
    CursorIterator<PersonalInfo> streamingIterator = getCursor("getPersonalInfo");
    assertThat(streamingIterator.getSize(), is(11));
    while (streamingIterator.hasNext()) {
      assertThat((streamingIterator.next()), isIn(INVOLVED_PEOPLE));
    }
  }

  @Test
  public void emptyPagedOperation() throws Exception {
    CursorIterator iterator = getCursor("emptyPagedOperation");
    assertThat(iterator.hasNext(), is(false));
    assertThat(iterator.getSize(), is(0));
    assertThat(closeEmptyOperationCalls, is(1));
  }

  @Test
  public void pagedOperationException() throws Exception {
    expectedException.expectCause(is(instanceOf(ConnectionException.class)));

    getCursor("failingPagedOperation").next();
  }

  @Test
  public void pagedOperationUsingConnection() throws Exception {
    Iterator iterator = getCursor("pagedOperationUsingConnection");
    assertThat(iterator.next().toString(), containsString(SAUL_NEW_NUMBER));
  }

  @Test
  public void pagedOperationWithStickyConnection() throws Exception {
    Iterator<Integer> iterator = getCursor("pagedOperationWithStickyConnection");
    Integer connectionId1 = iterator.next();
    Integer connectionId2 = iterator.next();

    assertThat(connectionId1, equalTo(connectionId2));
  }

  @Test
  public void pagedOperationWithExtensionClassLoader() throws Exception {
    Iterator iterator = getCursor("pagedOperationWithExtensionClassLoader");
    assertThat(iterator.next(), is(1));
  }

  @Test
  public void pagingProviderIsClosedSafelyDuringExceptionOnFirstPage() throws Exception {
    resetCounters();
    Iterator iterator = getCursorWithPayload("failAtClosePagedOperation", 1);
    iterator.next();
    assertThat("Paging provider was not closed.", closePagingProviderCalls, is(1));
  }

  @Test
  public void pagingProviderIsClosedSafelyAfterDataSourceIsFullyConsumed() throws Exception {
    resetCounters();
    flowRunner("consumeFailAtClosePagedOperation").withPayload(4).run();
    assertThat("Paging provider was not closed.", closePagingProviderCalls, is(1));
  }

  private <T> CursorIterator<T> getCursorWithPayload(String flowName, Object payload) throws Exception {
    CursorIteratorProvider provider =
        (CursorIteratorProvider) flowRunner(flowName).keepStreamsOpen().withPayload(payload).run().getMessage().getPayload()
            .getValue();

    return provider.openCursor();
  }

  private <T> CursorIterator<T> getCursor(String flowName) throws Exception {
    return getCursorWithPayload(flowName, "");
  }

  public static void resetCounters() {
    closePagingProviderCalls = 0;
    getPageCalls = 0;
  }
}
