/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.processor;

/**
 * This interface defines a contract for a component able to name staged queues
 * through a {@link org.mule.api.processor.StageNameSource} implementation
 *
 * @since 3.5.0
 */
public interface StageNameSourceProvider
{

    /**
     * Provides a {@link org.mule.api.processor.StageNameSource}
     *
     * @return a {@link org.mule.api.processor.StageNameSource}
     */
    public StageNameSource getAsyncStageNameSource();

    /**
     * Returns a {@link org.mule.api.processor.StageNameSource} that
     * takes the given paramter into consideration when generating the name
     *
     * @param asyncName a name to be consider when building the final name
     * @return a {@link org.mule.api.processor.StageNameSource}
     */
    public StageNameSource getAsyncStageNameSource(String asyncName);

}
