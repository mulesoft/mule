/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

/**
 * <code>Disposable</code> is a lifecycle interface that gets called at the dispose
 * lifecycle stage of the implementing service as the service is being destroyed.
 */
public interface Disposable
{
    String PHASE_NAME = "dispose";
    
    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    void dispose();
}
