/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.impl.model.AbstractModel;

/**
 * Creates a model suitable for Jca execution
 */
public class JcaModel extends AbstractModel
{
    public static final String JCA_MODEL_TYPE = "jca";

    public String getType()
    {
        return JCA_MODEL_TYPE;
    }
}
