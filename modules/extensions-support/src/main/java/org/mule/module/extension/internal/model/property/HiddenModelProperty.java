/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

/**
 * A custom model property to signal that a given component should not
 * be advertised.
 * <p/>
 * Because this class is stateless and there's no value in having many instances
 * of it, it has been defined as a singleton which instance is to be accessed with
 * through the {@link HiddenModelProperty#INSTANCE} field.
 *
 * @since 4.0
 */
public final class HiddenModelProperty
{

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = HiddenModelProperty.class.getName();

    public static final HiddenModelProperty INSTANCE = new HiddenModelProperty();

    private HiddenModelProperty()
    {
    }
}
