/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.internal.matcher;

import org.mule.module.extension.file.FilePayload;

import java.util.function.Predicate;

public final class NullFilePayloadPredicate implements Predicate<FilePayload>
{

    @Override
    public boolean test(FilePayload filePayload)
    {
        return true;
    }
}
