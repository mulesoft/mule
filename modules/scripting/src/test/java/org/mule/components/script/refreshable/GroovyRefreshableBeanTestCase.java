/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script.refreshable;


public class GroovyRefreshableBeanTestCase extends AbstractRefreshableBeanTestCase
{

    public static final String RECEIVED = "Received";
    public static final String RECEIVED2 = "Received2";
    public static final String PAYLOAD = "Test:";
    public static final String NAME_CALLABLE = "groovy-dynamic-script-callable.groovy";
    public static final String NAME_BEAN = "groovy-dynamic-script-bean.groovy";
    public static final String NAME_CHANGE_INTERFACE = "groovy-dynamic-script.groovy";
    public static final String ON_CALL_RECEIVED = "import org.mule.umo.UMOEventContext; import org.mule.umo.lifecycle.Callable; public class GroovyDynamicScript implements Callable { public Object onCall(UMOEventContext eventContext) throws Exception{ return eventContext.getMessage().getPayloadAsString() + \"" + RECEIVED + "\"; }}";
    public static final String ON_CALL_RECEIVED2 = ON_CALL_RECEIVED.replaceAll(RECEIVED, RECEIVED2);
    public static final String RECEIVE_RECEIVED = "public class GroovyDynamicScript { public String receive(String src) { return src + \"" + RECEIVED + "\"; }}";
    public static final String RECEIVE_RECEIVED2 = RECEIVE_RECEIVED.replaceAll(RECEIVED, RECEIVED2);

    protected String getConfigResources()
    {
        return "groovy-refreshable-config.xml";
    }

    public void testFirstOnCallRefresh() throws Exception
    {
        runScriptTest(ON_CALL_RECEIVED, NAME_CALLABLE, "vm://groovy_refresh_callable", PAYLOAD, RECEIVED);
    }
    
    public void testCallFirstTest() throws Exception
    {
        testFirstOnCallRefresh();
    }
    
    public void testSecondOnCallRefresh() throws Exception
    {
        runScriptTest(ON_CALL_RECEIVED2, NAME_CALLABLE, "vm://groovy_refresh_callable", PAYLOAD, RECEIVED2);
    }

    public void testFirstPojoRefresh() throws Exception
    {
        runScriptTest(RECEIVE_RECEIVED, NAME_BEAN, "vm://groovy_refresh_bean", PAYLOAD, RECEIVED);
    }
    
    public void testSecondPojoRefresh() throws Exception
    {
        runScriptTest(RECEIVE_RECEIVED2, NAME_BEAN, "vm://groovy_refresh_bean", PAYLOAD, RECEIVED2);
    }
    
    public void testFirstChangeInterfaces() throws Exception
    {
        runScriptTest(ON_CALL_RECEIVED, NAME_CHANGE_INTERFACE, "vm://groovy_refresh_changeInterfaces", PAYLOAD, RECEIVED);
    }
    
    public void testSecondChangeInterfaces() throws Exception
    {
        runScriptTest(RECEIVE_RECEIVED2, NAME_CHANGE_INTERFACE, "vm://groovy_refresh_changeInterfaces", PAYLOAD, RECEIVED2);
    }

}



