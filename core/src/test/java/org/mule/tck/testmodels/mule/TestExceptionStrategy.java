/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.DefaultExceptionStrategy;

/**
 * <code>TestExceptionStrategy</code> is used by the Mule test cases as a direct replacement of the 
 * {@link org.mule.DefaultExceptionStrategy}. This is used to test that overriding the default 
 * Exception strategy works.
 */
public class TestExceptionStrategy extends DefaultExceptionStrategy
{
    private ExceptionCallback callback;
    
    private String testProperty;

    public String getTestProperty()
    {
        return testProperty;
    }

    public void setTestProperty(String testProperty)
    {
        this.testProperty = testProperty;
    }
/*    
    protected void defaultHandler(Throwable t)
    {
        super.defaultHandler(t);
        if(callback != null)
        {
            callback.onException(t);
        }
    }
*/
    
    @Override
    public void exceptionThrown(Exception e)
    {
        if (callback != null)
        {
            callback.onException(e);
        }
    }

    public interface ExceptionCallback
    {
        void onException(Throwable t);
    }

    public void setExceptionCallback(ExceptionCallback exceptionCallback)
    {
        this.callback = exceptionCallback;        
    }
}
