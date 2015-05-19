/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

/**
 * Interface to model an action that should be executed
 * after a certain processing ends.
 * <p/>
 * Such functionality is executed by invoking the
 * {@link #postProcess()} method. It's also
 * possible to specify if the post processing
 * should be enabled or not through the
 * {@link #setPostProcessActionEnabled(boolean)}
 * method
 *
 * @since 3.7.0
 */
public interface PostProcessAction
{

    /**
     * Executes post processing actions
     *
     * @throws Exception
     */
    void postProcess() throws Exception;

    /**
     * Allows to enable/disable post processing
     *
     * @param postProcessActionEnabled a {@link boolean}
     */
    void setPostProcessActionEnabled(boolean postProcessActionEnabled);
}
