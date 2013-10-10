/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
