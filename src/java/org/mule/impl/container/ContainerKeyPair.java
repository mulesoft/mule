//COPYRIGHT
package org.mule.impl.container;

//AUTHOR

public class ContainerKeyPair
{
    private String contaimerName;
    private Object key;

    public ContainerKeyPair(String contaimerName, Object key) {
        this.contaimerName = contaimerName;
        this.key = key;
    }

    public String getContaimerName() {
        return contaimerName;
    }

    public Object getKey() {
        return key;
    }

    //here we only return the key value as string so that
    //containers that have no notion of this object can still
    //look up objects by calling the toString method on this object
    public String toString() {
        return key.toString();
    }
}
