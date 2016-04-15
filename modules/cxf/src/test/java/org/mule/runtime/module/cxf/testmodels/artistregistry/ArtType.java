/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.testmodels.artistregistry;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "artType")
@XmlEnum
public enum ArtType
{
    ACTOR,
    DIRECTOR,
    AUTHOR,
    PAINTER;

    public String value()
    {
        return name();
    }

    public static ArtType fromValue(String value)
    {
        return valueOf(value);
    }

}
