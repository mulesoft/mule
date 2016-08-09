/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.components.script.refreshable;


import org.junit.Test;

public class GroovyRefreshableBeanTestCase extends AbstractRefreshableBeanTestCase {

  public static final String RECEIVED = "Received";
  public static final String RECEIVED2 = "Received2";
  public static final String PAYLOAD = "Test:";
  public static final String NAME_CALLABLE = "groovy-dynamic-script-callable.groovy";
  public static final String NAME_BEAN = "groovy-dynamic-script-bean.groovy";
  public static final String NAME_CHANGE_INTERFACE = "groovy-dynamic-script.groovy";
  public static final String ON_CALL_RECEIVED =
      "import org.mule.runtime.core.api.MuleEventContext; import org.mule.runtime.core.api.lifecycle.Callable; public class GroovyDynamicScript implements Callable { public Object onCall(MuleEventContext eventContext) throws Exception{ return eventContext.getMessage().getPayload() + \""
          + RECEIVED + "\"; }}";
  public static final String ON_CALL_RECEIVED2 = ON_CALL_RECEIVED.replaceAll(RECEIVED, RECEIVED2);
  public static final String RECEIVE_RECEIVED =
      "public class GroovyDynamicScript { public String receive(String src) { return src + \"" + RECEIVED + "\"; }}";
  public static final String RECEIVE_RECEIVED2 = RECEIVE_RECEIVED.replaceAll(RECEIVED, RECEIVED2);

  @Override
  protected String getConfigFile() {
    return "groovy-refreshable-config-flow.xml";
  }

  @Test
  public void testFirstOnCallRefresh() throws Exception {
    runScriptTest(ON_CALL_RECEIVED, NAME_CALLABLE, "GroovyUMO_Callable", PAYLOAD, RECEIVED);
  }

  @Test
  public void testCallFirstTest() throws Exception {
    testFirstOnCallRefresh();
  }

  @Test
  public void testSecondOnCallRefresh() throws Exception {
    runScriptTest(ON_CALL_RECEIVED2, NAME_CALLABLE, "GroovyUMO_Callable", PAYLOAD, RECEIVED2);
  }

  @Test
  public void testFirstPojoRefresh() throws Exception {
    runScriptTest(RECEIVE_RECEIVED, NAME_BEAN, "GroovyUMO_Bean", PAYLOAD, RECEIVED);
  }

  @Test
  public void testSecondPojoRefresh() throws Exception {
    runScriptTest(RECEIVE_RECEIVED2, NAME_BEAN, "GroovyUMO_Bean", PAYLOAD, RECEIVED2);
  }

  @Test
  public void testFirstChangeInterfaces() throws Exception {
    runScriptTest(ON_CALL_RECEIVED, NAME_CHANGE_INTERFACE, "GroovyUMO_ChangeIntefaces", PAYLOAD, RECEIVED);
  }

  @Test
  public void testSecondChangeInterfaces() throws Exception {
    runScriptTest(RECEIVE_RECEIVED2, NAME_CHANGE_INTERFACE, "GroovyUMO_ChangeIntefaces", PAYLOAD, RECEIVED2);
  }

}


