# Apigee Edge Signed URL callout

This directory contains the Java source code and Java jars required to compile a Java
callout for Apigee Edge that produces a signed URL, according to [this specification](https://cloud.google.com/storage/docs/access-control/signed-urls). 

You do not need to build this Java code in order to use the JWT Generator or Verifier
callout.  The callout will work, with the pre-built JAR file.  Find the pre-built JAR
file in (the API Proxy subdirectory)[../apiproxy/resources/java].

You may wish to modify this code for your own purposes. In that case,
you can modify the Java code, re-build, then copy that JAR into the
appropriate apiproxy/resources/java directory for your API Proxy.


## What Good is This?

Suppose you need to generate a URL for Google Cloud Storage resource, and expose it to someone else. You can do that with "signed URLs".  If you want to do this from within an Apigee Edge proxy, this callout might help. 

## What kind of Signing? 

The signing prescribed by Google is RSASSA with PKCS1.5 padding. 

## Using the Jar

You do not need to build the JAR in order to use it.
To use it:

1. Include the Java callout policy in your
   apiproxy/resources/policies directory. The configuration should look like
   this:
    ```xml
    <JavaCallout name="Java-SignedUrlGenerator" >
      <Properties>...</Properties>
      <ClassName>com.google.apigee.edgecallouts.rsa.SignedUrlCallout</ClassName>
      <ResourceURL>java://edge-google-signed-url-1.0.4.jar</ResourceURL>
    </JavaCallout>
   ```

2. Deploy your API Proxy, using
   [pushapi](https://github.com/carloseberhardt/apiploy), [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js),
   [the Import-EdgeApi cmdlet for Powershell](https://github.com/DinoChiesa/Edge-Powershell-Admin/blob/develop/PSApigeeEdge/Public/Import-EdgeApi.ps1),
   or another similar tool.

For some examples of how to configure the callout, see [the related api proxy bundle](../apiproxy).


## Dependencies

Jars available in Edge:
 - Apigee Edge expressions v1.0
 - Apigee Edge message-flow v1.0

Jars not available in Edge:
 - Apache commons lang3
 - Bouncy Castle 1.54

Maven will download all of these dependencies for you. If for some
reason you want to download these dependencies manually, you can visit
<a href='https://mvnrepository.com'>https://mvnrepository.com</a> .




## License

This project and all the code contained within is Copyright 2017-2018 Google LLC, and is licensed under the [Apache 2.0 Source license](LICENSE).


## Limitations

??

