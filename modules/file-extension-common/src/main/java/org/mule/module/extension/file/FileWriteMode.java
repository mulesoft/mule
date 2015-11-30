/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

/**
 * List different strategies regarding how to write new files
 *
 * @since 4.0
 */
public enum FileWriteMode
{
    /**
     * Means that if the file to be written already exists, then it should
     * be overwritten
     */
    OVERWRITE
            {
                @Override
                public OpenOption[] getOpenOptions()
                {
                    return new OpenOption[] {CREATE, WRITE, TRUNCATE_EXISTING};
                }
            },
    /**
     * Means that if the file to be written already exists, then the content
     * should be appended to that file
     */
    APPEND
            {
                @Override
                public OpenOption[] getOpenOptions()
                {
                    return new OpenOption[] {CREATE, WRITE, StandardOpenOption.APPEND};
                }
            },
    /**
     * Means that a new file should be created and an error should be raised
     * if the file already exists
     */
    CREATE_NEW
            {
                @Override
                public OpenOption[] getOpenOptions()
                {
                    return new OpenOption[] {WRITE, StandardOpenOption.CREATE_NEW};
                }
            };

    public abstract OpenOption[] getOpenOptions();
}
