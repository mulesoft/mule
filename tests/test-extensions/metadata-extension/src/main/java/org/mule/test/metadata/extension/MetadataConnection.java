/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import java.util.Arrays;
import java.util.List;

public class MetadataConnection
{

    public static final String PERSON = "PERSON";
    public static final String CAR = "CAR";
    public static final String HOUSE = "HOUSE";

    public List<String> getEntities()
    {
        return Arrays.asList(PERSON, CAR, HOUSE);
    }

}
