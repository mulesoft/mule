package org.mule.api.registry;

/**
 * TODO
 */
public enum ServiceType
{
    TRANSPORT("transport", "org/mule/transport"),
    MODEL("model", "org/mule/model"),
    EXCEPTION("exception", "org/mule/config");

    private String name;
    private String path;


    ServiceType(String name, String path)
    {
        this.name = name;
        this.path = path;
    }

    @Override
    public String toString()
    {
        return name + ": " + path;
    }

    public String getPath()
    {
        return path;
    }

    public String getName()
    {
        return name;
    }
}


