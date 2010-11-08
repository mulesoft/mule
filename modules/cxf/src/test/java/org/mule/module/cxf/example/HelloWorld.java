package org.mule.module.cxf.example;

import javax.jws.WebService;

@WebService
public interface HelloWorld {
    String sayHi(String text);
}