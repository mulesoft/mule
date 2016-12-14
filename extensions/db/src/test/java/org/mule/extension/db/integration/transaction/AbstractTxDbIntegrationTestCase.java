/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.transaction;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import org.hamcrest.Matcher;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.exception.MessagingException;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

public abstract class AbstractTxDbIntegrationTestCase extends AbstractDbIntegrationTestCase {

  protected static final String MARS = "Mars";
  protected static final String MERCURY = "Mercury";

  protected Map<String, Object> additionalVariables() {
    return emptyMap();
  }

  protected void executeTransaction(String flowName) throws Exception {
    FlowRunner flowRunner = flowRunner(flowName);
    additionalVariables().entrySet().forEach(entry -> flowRunner.withVariable(entry.getKey(), entry.getValue()));
    MessagingException exception = flowRunner.runExpectingException();
    assertThat(exception.getCause(), is(instanceOf(ComponentException.class)));
  }

  protected void validateDbState(String planet) throws java.sql.SQLException {
    DataSource dataSource = getDefaultDataSource();
    checkState(planet, dataSource);
  }

  protected void validateDbState(String planet, String configName) throws java.sql.SQLException {
    DataSource dataSource = getDefaultDataSource(configName);
    checkState(planet, dataSource);
  }

  private void checkState(String planet, DataSource dataSource) throws java.sql.SQLException {
    List<Map<String, Object>> result = selectData("select * from PLANET where POSITION=4", dataSource);
    Matcher<Map<? extends String, ?>> mapMatcher = hasEntry("NAME", planet);
    Matcher<Map<? extends String, ?>> numberMatcher = hasEntry("POSITION", 4);
    Matcher<Iterable<Map<String, Object>>> matcher = hasItems(mapMatcher, numberMatcher);
    assertThat(result, matcher);
  }
}
