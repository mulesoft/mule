/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.streaming;

/**
 * Object provides a hint on total amount of elements it has/can process
 * 
 * @since 3.5.0
 */
public interface ProvidesTotalHint
{

    /**
     * returns the total amount of items available for storage/processing. In some
     * scenarios, it might not be possible/convenient to actually retrieve this value
     * or it might not be available at this point. -1 is returned in such a case.
     */
    public int size();
}
