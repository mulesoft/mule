/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.mime.TestMtomService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MtomTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public MtomTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mtom-conf-service.xml"},
            {ConfigVariant.FLOW, "mtom-conf-flow.xml"}
        });
    }      

    @Test
    public void testEchoService() throws Exception
    {
        URL wsdl = getClass().getResource("/wsdl/mtom_xop.wsdl");
        assertNotNull(wsdl);

        CxfConfiguration clientConfig = new CxfConfiguration();
        clientConfig.setMuleContext(muleContext);
        clientConfig.initialise();
        BusFactory.setThreadDefaultBus(clientConfig.getCxfBus());

        TestMtomService svc = new TestMtomService(wsdl);

        TestMtom port = svc.getTestMtomPort();

        BindingProvider bp = ((BindingProvider) port);
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "http://localhost:" + dynamicPort.getNumber() + "/services/mtom");
        ((SOAPBinding) bp.getBinding()).setMTOMEnabled(true);
        // Client client = ClientProxy.getClient(port);
        // new LoggingFeature().initialize(client, null);

        File file = new File("src/test/resources/mtom-conf-service.xml");
        DataHandler dh = new DataHandler(new FileDataSource(file));

        Holder<String> name = new Holder<String>("test");
        Holder<DataHandler> info = new Holder<DataHandler>(dh);

        port.testXop(name, info);

        assertEquals("return detail + test", name.value);
        assertNotNull(info.value);

        InputStream input = info.value.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(input, bos);
        input.close();
    }
}

