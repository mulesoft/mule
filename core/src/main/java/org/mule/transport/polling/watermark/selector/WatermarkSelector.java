/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark.selector;

/**
 * A watermark selector receives values and selects which one should be the new
 * watermark value
 * 
 * @since 3.5.0
 */
public abstract class WatermarkSelector
{

    /**
     * The selected value
     */
    protected Object value;

    /**
     * Receives a value which is candidate to become the new watermark value. No
     * thread-safeness is guaranteed by this contract. It's up to each
     * client/implementation to handle that
     */
    public abstract void acceptValue(Object value);

    /**
     * Returns the selected value. This contract does not guarantee idempotency.
     * Continuous invocations to this method might return different values if
     * {@link #acceptValue(Object)} is invoked in between. Thread-safeness is also
     * not guaranteed by this contract
     * 
     * @return the selected value
     */
    public Object getSelectedValue()
    {
        return this.value;
    }

    /**
     * Returns this selector to a blank state so that it can be reused
     */
    public void reset()
    {
        this.value = null;
    }

}
