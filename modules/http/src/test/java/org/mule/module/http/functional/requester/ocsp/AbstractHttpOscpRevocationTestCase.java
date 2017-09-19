package org.mule.module.http.functional.requester.ocsp;

import static java.lang.String.format;
import static org.junit.Assert.fail;
import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;
import org.mule.util.concurrent.Latch;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

public abstract class AbstractHttpOscpRevocationTestCase extends AbstractHttpTlsRevocationTestCase
{


    private static String RUN_OCSP_SERVER_COMMAND = "openssl ocsp " +
                                                    "-index src/test/resources/tls/ocsp/%s " +
                                                    "-CA src/test/resources/tls/ocsp/server.crt " +
                                                    "-rkey src/test/resources/tls/ocsp/server_key.pem " +
                                                    "-port 1111 " +
                                                    "-rsigner src/test/resources/tls/ocsp/server.crt";

    static String REVOKED_OCSP_LIST = "revoked-ocsp.txt";

    static String VALID_OCSP_LIST = "valid-ocsp.txt";

    /**
     *  This certified entity was generated to test revocation with OCSP mechanism.
     */
    static String ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH = "entity4";

    private Process process ;

    final Latch serverLatch = new Latch();

    private final String ocspList ;


    AbstractHttpOscpRevocationTestCase(String entityCertified, String ocspList)
    {
        super("http-requester-ocsp-revocation-config.xml", entityCertified);
        this.ocspList = ocspList;
    }


    @Before
    public void setUp() throws Exception
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    process = Runtime.getRuntime().exec(format(RUN_OCSP_SERVER_COMMAND, ocspList));
                    serverLatch.countDown();
                }
                catch (IOException e)
                {
                    fail("There was an error trying to start the ocsp server.");
                }
            }
        }).start();
    }

    @After
    public void tearDown() throws IOException
    {
        process.destroy();
    }

}
