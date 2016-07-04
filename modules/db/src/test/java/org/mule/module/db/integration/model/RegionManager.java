/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import static org.mule.module.db.integration.model.Region.NORTHWEST;
import static org.mule.module.db.integration.model.Region.SOUTHWEST;

public class RegionManager
{

    public static final RegionManager SOUTHWEST_MANAGER = new RegionManager(SOUTHWEST.getName(), "Manager1", new ContactDetails("home", "1-111-111", "1@1111.com"));
    public static final RegionManager NORTHWEST_MANAGER = new RegionManager(NORTHWEST.getName(), "Manager2", new ContactDetails("work", "2-222-222", "2@2222.com"));

    private String regionName;
    private String name;
    private ContactDetails contactDetails;

    public RegionManager(String regionName, String name, ContactDetails contactDetails)
    {
        this.regionName = regionName;
        this.name = name;
        this.contactDetails = contactDetails;
    }

    public String getRegionName()
    {
        return regionName;
    }

    public String getName()
    {
        return name;
    }

    public ContactDetails getContactDetails()
    {
        return contactDetails;
    }
}
