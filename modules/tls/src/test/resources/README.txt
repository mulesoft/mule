How to generate the keystores for unit tests:

1) generate the client keystore
keytool -genkey -alias muleclient -keystore clientKeystore -storepass mulepassword -validity 9999 -dname "CN=localhost, O=Mule Runtime, C=US"

2) export the client certificate
keytool -export -alias muleclient -keystore clientKeystore -storepass mulepassword -file muleclient.cer

3) generate the server keystore
keytool -genkey -alias muleserver -keystore serverKeystore -storepass mulepassword -validity 9999 -dname "CN=localhost, O=Mule Runtime, C=US"

4) export the server certificate
keytool -export -alias muleserver -keystore serverKeystore -storepass mulepassword -file muleserver.cer

5) import the server certificate into the client keystore
keytool -import -alias muleserver -file muleserver.cer -keystore clientKeystore -storepass mulepassword

6) import the client certificate into the server keystore
keytool -import -alias muleclient -file muleclient.cer -keystore serverKeystore -storepass mulepassword

7) create the trust store
keytool -genkey -alias delme -keystore trustStore -storepass mulepassword -validity 9999 -dname "CN=delme, O=Mule Runtime, C=US"
keytool -delete -alias delme -keystore trustStore -storepass mulepassword

8) import client and server certificate into the trust store
keytool -import -alias muleclient -file muleclient.cer -keystore trustStore -storepass mulepassword
keytool -import -alias muleserver -file muleserver.cer -keystore trustStore -storepass mulepassword
