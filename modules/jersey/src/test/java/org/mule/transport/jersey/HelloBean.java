package org.mule.transport.jersey;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HelloBean {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
}
