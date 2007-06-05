Generating a key for the unit tests
===================================

(I'm putting the info here to be as close as possible to the cause of the problem
i.e. the keystore)

To generate a new certificate for the unit tests issue the following command:
    keytool -genkey -keyalg RSA -alias mulealias \
    -dname 'CN=Mule Source, OU=Unknown, O=Mule Source, L=Unknown, ST=Unknown, C=Unknown' \
    -validity 999 -keypass mulepassword -keystore private.keystore -storepass mulepassword

Key to success is generating an RSA key as the standard DSA does not seem to work with
wss4j.

-dirk (dirk@mulesource.com)
