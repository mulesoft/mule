package org.mule.tck.external.applications;

public interface TestService
{
    public Test[] getTests();
    public Test getTest(String key) throws Exception;
}
