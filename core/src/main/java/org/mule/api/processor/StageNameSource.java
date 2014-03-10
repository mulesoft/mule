/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

/**
 * A component capable of generating names for staged queues.
 * Subsequent invocations to {@link #getName()} are not required to consistently
 * return the same value. On the contrary, there cases in which implementations will need not to do that.
 * Implementations of this interface should be thread-safe
 *
 * @since 3.5.0
 */
public interface StageNameSource
{

    /**
     * Generates the name for a staged queue. Subsequent invocations
     * to this method are not required to consistently return the same value
     *
     * @return a {@link java.lang.String}
     */
    public String getName();

}
