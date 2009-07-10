/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.rmi;

import org.mule.tck.services.MatchingMethodsComponent;

import java.io.Serializable;

public class SerializedMatchingMethodsComponent extends MatchingMethodsComponent implements Serializable
{
    private static final long serialVersionUID = 6237629879756233705L;
}
