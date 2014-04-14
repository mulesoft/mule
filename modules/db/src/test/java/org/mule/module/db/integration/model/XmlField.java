/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import org.custommonkey.xmlunit.XMLUnit;

public class XmlField extends Field
{

    public XmlField(String name, Object value)
    {
        super(name, value);
    }

    @Override
    protected boolean checkEqualValues(Field field)
    {
        if (getValue().equals(field.getValue()))
        {
            return true;
        }
        else
        {
            try
            {
                XMLUnit.setIgnoreWhitespace(true);

                return XMLUnit.compareXML((String) getValue(), (String) field.getValue()).identical();
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }
}
