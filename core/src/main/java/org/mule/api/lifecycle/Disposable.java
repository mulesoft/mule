/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
