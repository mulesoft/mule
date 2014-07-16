/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.cep;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * A helper class to load and return the list of companies
 * 
 * @author etirelli
 */
public class CompanyRegistry
{
    public static Collection<Company> getCompanies()
    {
        HashMap<String, Company> companies = new HashMap<String, Company>();
        companies.put("RHT", new Company("Red Hat Inc", "RHT"));
        companies.put("JAVA", new Company("Sun Microsystems", "JAVA"));
        companies.put("MSFT", new Company("Microsoft Corp", "MSFT"));
        companies.put("ORCL", new Company("Oracle Corp", "ORCL"));
        companies.put("SAP", new Company("SAP", "SAP"));
        companies.put("GOOG", new Company("Google Inc", "GOOG"));
        companies.put("YHOO", new Company("Yahoo! Inc", "YHOO"));
        companies.put("IBM", new Company("IBM Corp", "IBM"));

        return Collections.unmodifiableCollection(companies.values());
    }
}
