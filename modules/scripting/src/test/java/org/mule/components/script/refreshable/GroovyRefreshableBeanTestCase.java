/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.script.refreshable;


public class GroovyRefreshableBeanTestCase extends AbstractRefreshableBeanTestCase
{

    public GroovyRefreshableBeanTestCase()
    {
        scriptPath_callable = "./target/test-classes/groovy-dynamic-script-callable.groovy";
        scriptPath_bean = "./target/test-classes/groovy-dynamic-script-bean.groovy";
        scriptPath_changeInterfaces = "./target/test-classes/groovy-dynamic-script.groovy"; 
        script1 = "import org.mule.umo.UMOEventContext; import org.mule.umo.lifecycle.Callable; public class GroovyDynamicScript implements Callable { public Object onCall(UMOEventContext eventContext) throws Exception{ return eventContext.getMessage().getPayloadAsString() + \" Received\"; }}";
        script2 = script1.replaceAll(" Received", " Received2");
        script3 = "public class GroovyDynamicScript { public String receive(String src) { return src + \" Received\"; }}";
        script4 = script3.replaceAll(" Received", " Received2");
        
    }
    protected String getConfigResources()
    {
        return "groovy-refreshable-config.xml";
    }

    
    public void testFirstOnCallRefresh() throws Exception
    {
        runScriptTest(script1, scriptPath_callable, "vm://groovy_refresh_callable", "Test:", "Test: Received");
    }
    
    public void testCallFirstTest() throws Exception
    {
        testFirstOnCallRefresh();
    }
    
    public void testFirstPojoRefresh() throws Exception
    {
        runScriptTest(script3, scriptPath_bean, "vm://groovy_refresh_bean", "Test:", "Test: Received");
    }
    
    public void testSecondPojoRefresh() throws Exception
    {
        runScriptTest(script4, scriptPath_bean, "vm://groovy_refresh_bean", "Test:", "Test: Received2");
    }
    
    public void testFirstChangeInterfaces() throws Exception
    {
        runScriptTest(script1, scriptPath_changeInterfaces, "vm://groovy_refresh_changeInterfaces", "Test:", "Test: Received");
    }
    
    public void testSecondChangeInterfaces() throws Exception
    {
        runScriptTest(script3, scriptPath_changeInterfaces, "vm://groovy_refresh_changeInterfaces", "Test:", "Test: Received");
    }
    
    

}



