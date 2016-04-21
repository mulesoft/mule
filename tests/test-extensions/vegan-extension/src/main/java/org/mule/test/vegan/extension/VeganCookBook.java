/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import java.util.List;

public class VeganCookBook
{
    List<String> recipes;

    Integer numberOfPages;

    public List<String> getRecipes()
    {
        return recipes;
    }

    public void setRecipes(List<String> recipes)
    {
        this.recipes = recipes;
    }

    public Integer getNumberOfPages()
    {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages)
    {
        this.numberOfPages = numberOfPages;
    }
}
