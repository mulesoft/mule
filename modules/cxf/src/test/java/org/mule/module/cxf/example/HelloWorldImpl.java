package org.mule.module.cxf.example;

import javax.jws.WebService;

@WebService(endpointInterface = "org.mule.module.cxf.example.HelloWorld",
            serviceName = "HelloWorld")
public class HelloWorldImpl implements HelloWorld {

    public String sayHi(String text) {
        return "Hello\u2297 " + text;
    }
}
