/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.util;

import org.mule.module.pgp.exception.MissingPGPKeyException;

public class ValidatorUtil
{

    public static void validateNotNull (Object object, org.mule.config.i18n.Message message) throws MissingPGPKeyException
    {
        if (object == null)
        {
            throw new MissingPGPKeyException(message);
        }
    }

}
