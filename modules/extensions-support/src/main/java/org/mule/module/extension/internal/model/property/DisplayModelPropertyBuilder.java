/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.extension.api.annotation.param.display.Placement;
import org.mule.extension.api.introspection.property.DisplayModelProperty;
import org.mule.extension.api.introspection.property.ImmutableDisplayModelProperty;

/**
 * Builder for {@link ImmutableDisplayModelProperty}
 * @since 4.0
 */
public class DisplayModelPropertyBuilder
{

    private String displayName;
    private boolean isPassword;
    private boolean isText;
    private int order;
    private String groupName;
    private String tabName;

    public static DisplayModelPropertyBuilder create()
    {
        return new DisplayModelPropertyBuilder();
    }

    public static DisplayModelPropertyBuilder create(DisplayModelProperty modelProperty)
    {
        return new DisplayModelPropertyBuilder(modelProperty.getDisplayName(),
                                               modelProperty.isPassword(),
                                               modelProperty.isText(),
                                               modelProperty.getOrder(),
                                               modelProperty.getTabName(),
                                               modelProperty.getGroupName());
    }

    private DisplayModelPropertyBuilder()
    {
        this.isPassword = false;
        this.isText = false;
        this.order = Placement.DEFAULT_ORDER;
        this.displayName = EMPTY;
        this.groupName = EMPTY;
        this.tabName = EMPTY;
    }

    public DisplayModelPropertyBuilder(String displayName, boolean isPassword, boolean isText, int order, String tabName, String groupName)
    {
        this.displayName = displayName;
        this.isPassword = isPassword;
        this.isText = isText;
        this.order = order;
        this.tabName = tabName;
        this.groupName = groupName;
    }


    public DisplayModelPropertyBuilder withPassword(boolean isPassword)
    {
        this.isPassword = isPassword;
        return this;
    }

    public DisplayModelPropertyBuilder withText(boolean isText)
    {
        this.isText = isText;
        return this;
    }

    public DisplayModelPropertyBuilder tabName(String tabName)
    {
        this.tabName = tabName;
        return this;
    }

    public DisplayModelPropertyBuilder groupName(String groupName)
    {
        this.groupName = groupName;
        return this;
    }

    public DisplayModelPropertyBuilder displayName(String displayName)
    {
        this.displayName = displayName;
        return this;
    }

    public DisplayModelPropertyBuilder order(int order)
    {
        this.order = order;
        return this;
    }

    public DisplayModelProperty build()
    {
        return new ImmutableDisplayModelProperty(displayName,
                                                 isPassword,
                                                 isText,
                                                 order,
                                                 groupName,
                                                 tabName);
    }

}
