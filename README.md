# Apigee Callout: Signed URL Generator

This is a simple callout for Apigee that generates a signed URL for Google Cloud Storage.

Google Cloud Storage allows apps to create [signed
URLs](https://cloud.google.com/storage/docs/access-control/signed-urls) with
expiry, to allow third parties a time-limited access to a resource, like an
object being stored by Google Cloud storage.

It works like this: the app authenticates the third party in some way; the app
generates a signed URL for the third party; the third party uses that signed
thing in an HTTP request for an object from Google Cloud storage; GCS
authenticates the signature before responding with the object content.

This repo includes a class that produces a
[V4 signature](https://cloud.google.com/storage/docs/access-control/signing-urls-manually),
and a separate class that produces a [V2 signature](https://cloud.google.com/storage/docs/access-control/signed-urls-v2).

> As of September 2019, Google is now recommending the "V4" signature
> method. There was a previous method called "v2" which may still be supported,
> but which Google is dis-recommending. By default, this callout creates V4
> signatures. You can configure it to alternatively create V2 signatures.

Whether using V4 or V2 signed URLs, an app must build a "String to Sign", which
includes information about the object being accessed and the access lifetime,
and then must sign that string with an RSA private key, using a SHA256
digest. Then the app encodes that signature and embeds the encoded version of
the signature into a url as a query parameter. The app can send that full URL to
the third party system, which can use the URL to connect directly with Google
Cloud Storage, for either PUT or GET operations.

This callout helps builds the URL, via all those steps.

## V2 vs V4

Comparing V2 to V4 signing, the "string to sign" is different, and the signedurl
itself is also different. Also the V4 signing has more strict limits on the
expiry. For more details see [this
link](https://stackoverflow.com/q/58145068/48082).

This is of historical interest only, because V2 signing is deprecated.

## Why Signed URLs?

Suppose you need to generate a URL for Google Cloud Storage resource, and expose
it to someone else, to allow that party to access the URL - for example to
upload a file, or to download a file - for only a limited period of time, with no other
authorization.

You can do that with "signed URLs".  If you want to do this from within an
Apigee proxy, this callout might help.

## Why use an Apigee callout rather than direct signing?

You could write your own code to build a signed URL. Any app that has access to
an RSA signing library could implement this signing itself. In the documentation
pages for V4 signing, Google provides examples in Python and Java, and maybe
other languages.

The advantage of using an Apigee callout to perform the signing is that Apigee
can then act as a security mediator. The private key used for signing can remain
secret, held within Apigee. Apigee can generate a signed URL and dispense it to
a validated, authenticated client application. The control and governance and
visibility you get with Apigee is a nice advantage for an API-centric
integration architecture.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## Status

This is a community supported project. There is no warranty for this code.
If you have problems or questions, as on [commmunity.apigee.com](https://community.apigee.com).

## Screencast

I've produced [an 18-minute screencast](https://youtu.be/rqpbspscbmM) walking through this example. To check it out, click here:
[![screencast thumbnail](./assets/thumbnail-20210512-135052.png)](https://youtu.be/rqpbspscbmM)

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is Copyright 2018-2021, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source. And, you don't need to compile it in order to use it.

## Configuration

To use V4 signing, configure the policy like this:

```
<JavaCallout name='Java-URL-Sign-V4-Example1'>
  <Properties>
    <Property name='service-account-key'>{my_service_account_json}</Property>
    <Property name='verb'>GET</Property>
    <Property name='resource'>{my_resource}</Property>
    <Property name='expires-in'>10m</Property> <!-- 10m = ten minutes -->
  </Properties>
  <ClassName>com.google.apigee.callouts.rsa.V4SignedUrlCallout</ClassName>
  <ResourceURL>java://apigee-google-signed-url-20210311.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the signature.

| name                 | required | meaning                                                           |
| -------------------- | -------- | ----------------------------------------------------------------- |
| service-account-key  | required | the contents of the [service account key file](https://cloud.google.com/iam/docs/creating-managing-service-account-keys) from Google. This is a JSON string containing the service account information, including the private key and client\_email.       |
| verb                 | required | the verb: GET, POST, etc                                          |
| resource             | optional | the full resource string, eg: /example-bucket/cat-pics/tabby.jpeg |
| bucket               | optional | the bucket name, eg example-bucket                                |
| object               | optional | the object, eg. cat-pics/tabby.jpeg                               |
| expires-in           | optional | a string representing _relative_ expiry.  10s, 5m, 2h, 3d.  With no character suffix, interpreted as "seconds". This interval is added to the current time to calculate expiry. For V4, the longest permitted expiration value is 604800 seconds (7 days). |
| expiry               | optional | a string representing expiry, in absolute seconds-since-epoch.    |
| addl-headers         | optional | a string of name:value pairs, separated by \|  |
| addl-query           | optional | a string of param=value pairs, separated by &  |
| payload              | optional | a string indicating the payload that will be used with the signed request. Empty for GET requests. |

For all properties, you can pass an explicit value or a variable reference,
which is a variable name surrounded by curlies, such
as {my-variable}.

Pass either `expires-in` or `expiry`. If you pass both, `expires-in` takes precedence.

Pass either `resource` or the combination of `bucket` and `object`.  If you pass
both, the logic will use what you pass for `resource`.

The output of the callout is a set of context variables:

| name                   | meaning                                                                            |
| ---------------------- | ---------------------------------------------------------------------------------- |
| sign\_signedurl        | the full signedurl                                                                 |
| sign\_signature        | the hex-encoded (aka base16-encoded) signature.                                    |
| sign\_expiration       | The expiration value, in seconds-since-epoch. Computed from NOW + expires-in. For diagnostic information.     |
| sign\_duration         | The duration, (expiration time - now), in seconds. For diagnostic information.     |
| sign\_expiration\_ISO  | An ISO-formatted string for the expiration. For diagnostics and human consumption. |


## More Complex Example of V4 Signing

Suppose you would like the signature to be valid for a request that includes an
additional header and some additional query parameters. That configuration would
look like this:

```
<JavaCallout name='Java-URL-Sign-V4-Example2'>
  <Properties>
    <Property name='service-account-key'>{my_service_account_json}</Property>
    <Property name='verb'>GET</Property>
    <Property name='resource'>{my_resource}</Property>
    <Property name='expires-in'>1h</Property> <!-- 1h = one hour -->
    <Property name='addl-headers'>content-type:application/octet-stream</Property>
    <!-- use CDATA for the below to escape the ampersands
    <Property name='addl-query'><![CDATA[a=foo&b=bar]]></Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.rsa.V4SignedUrlCallout</ClassName>
  <ResourceURL>java://apigee-google-signed-url-20210311.jar</ResourceURL>
</JavaCallout>
```

## Examples

See the attached [bundle](./bundle) for a working API Proxy.
To use it,

1. deploy it to any org and environment.
2. create an environment-scoped KVM called "settings"
3. use Google Cloud Console to create a service account, download a key in .json format, and grant the account "Object owner" access to the GCS bucket. 

Then invoke it like this:

### V4 Example
```
ORG=myorg
ENV=myenv
endpoint=https://$ORG-$ENV.apigee.net
curl -i $endpoint/signurl/v4/example
```

You should see a signature upon output:

```
HTTP/1.1 200 OK
Date: Thu, 24 Oct 2019 22:44:41 GMT
Content-Type: application/json
Content-Length: 1556
Connection: keep-alive

{
  "status" : "ok",
  "resource" : "/example-bucket/cat-pics/tabby.png",
  "signature" : "58467c906c7accf8f98bb2ca4de64b5dbb83159e225794b51572fb361af98e3517103bfd63b9295608f00cdbb70123edbed1dd6f6a317671e93ae6ec6818278bc16e9ca3fa2e6f943c9e0822e6ce51fffc69d947919ddfac00b919917631990885a8faae2d1041e09d5167686d2751ff95a38145f91f09dec35a7a1515456b771d94ba943d73db15dd3819a650cf97f706fc09edbfc17a29ff6b9bb6b44e88f26e658e9e77090de9734d0224771cad1659f433f2276740210ea3ccca9057306c708b60123da717f7b66475757867ad87073065ffcba7adfa4349778847d00f630012082bef98556514112a853735084762054e1354b75ce3801960022ccfe74f",
  "expiration" : {
    "seconds" : 1571957681,
    "relative" : 600,
    "ISO" : "2019-10-24T22:54:41Z"
  },
  "signedurl" : "https://storage.googleapis.com/example-bucket/cat-pics/tabby.png?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=account-223456789%40project-apigee.iam.gserviceaccount.com%2F20191024%2Fus%2Fstorage%2Fgoog4_request&X-Goog-Date=20191024T224441Z&X-Goog-Expires=600&X-Goog-SignedHeaders=host&X-Goog-Signature=58467c906c7accf8f98bb2ca4de64b5dbb83159e225794b51572fb361af98e3517103bfd63b9295608f00cdbb70123edbed1dd6f6a317671e93ae6ec6818278bc16e9ca3fa2e6f943c9e0822e6ce51fffc69d947919ddfac00b919917631990885a8faae2d1041e09d5167686d2751ff95a38145f91f09dec35a7a1515456b771d94ba943d73db15dd3819a650cf97f706fc09edbfc17a29ff6b9bb6b44e88f26e658e9e77090de9734d0224771cad1659f433f2276740210ea3ccca9057306c708b60123da717f7b66475757867ad87073065ffcba7adfa4349778847d00f630012082bef98556514112a853735084762054e1354b75ce3801960022ccfe74f"
}
```

This is just a sample. This signature is the right format, but it won't be valid. That's because the
bucket name and object name are fake, and the key used to sign is not registered
with GCS. The bucket, object, and key are _ALL SAMPLE DATA_.


#### Valid Signed URL for GET

To actually produce a valid signed url, you need to invoke the callout with
valid data for the bucket, object and key. If you have downloaded a service
account .json file, and you have a GCS bucket and object, try it like this:

First load your actual service account key into the encrypted KVM (This is a one-time operation):

```
curl -i $endpoint/signurl/kvm/sakey \
  -H content-type:application/json \
  -X POST \
  --data-binary @service-account-credentials.json
```

Then, invoke the proxy to generate the signed URL.
```
curl -i $endpoint/signurl/v4/real \
  -H content-type:application/x-www-form-urlencoded \
  -X POST -d bucket=your-bucket -d object=your-object
```

The resulting URL will be usable in a browser to GET the object.

#### Upload

If you want generate a signed URL that allows upload, also specify a `verb` form parameter with the value `PUT`:

```
curl -i $endpoint/signurl/v4/real \
  -H content-type:application/x-www-form-urlencoded \
  -X POST -d bucket=your-bucket -d object=your-object -d verb=PUT
```

The resulting URL will be usable to PUT an object with the given name, into the bucket.

#### Upload with a given content-type

If you want generate a signed URL that allows upload of an object with a specific content-type, also specify a `ctype` form parameter:

```
curl -i $endpoint/signurl/v4/real \
  -H content-type:application/x-www-form-urlencoded \
  -X POST -d bucket=your-bucket -d object=my-image.png -d verb=PUT -d ctype=application/png
```

The resulting URL will be usable to PUT an object with the given name and given content-type, into the GCS bucket.


## Configuration for V2 Signing (DEPRECATED)

To use V2 signing (which is now deprecated by Google), configure the policy like this:

```
<JavaCallout name='Java-URL-Sign-V2'>
  <Properties>
    <Property name='private-key'>{my_private_key}</Property>
    <Property name='private-key-password'>{my_private_key_password}</Property>
    <Property name='verb'>GET</Property>
    <Property name='resource'>{my_resource}</Property>
    <Property name='expires-in'>10m</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.rsa.V2SignedUrlCallout</ClassName>
  <ResourceURL>java://apigee-google-signed-url-20210311.jar</ResourceURL>
</JavaCallout>
```

You can see this configuration specifies a different class name.

The V2 class accepts different Properties, for the various inputs for the signature.

| name                 | required | meaning                                                           |
| -------------------- | -------- | ----------------------------------------------------------------- |
| service-account-key  | required | the contents of the [service account key file](https://cloud.google.com/iam/docs/creating-managing-service-account-keys) from Google. This is a JSON string containing the service account information, including the private key and client\_email.       |
| verb                 | required | the verb: GET, POST, etc                                          |
| resource             | optional | the resource string, eg: /example-bucket/cat-pics/tabby.jpeg      |
| bucket               | optional | the bucket name, eg example-bucket                                |
| object               | optional | the object, eg. cat-pics/tabby.jpeg                               |
| expires-in           | optional | a string representing _relative_ expiry, eg. 10s, 5m, 2h, 3d.  With no character suffix, interpreted as "seconds". |
| expiry               | optional | a string representing expiry, in absolute seconds-since-epoch.    |
| content-md5          | optional | the MD5 checksum the client must pass.                            |
| content-type         | optional | content-type header, as above.                                    |


Pass either `expires-in` or `expiry`. If you pass both, `expires-in` takes precedence.

Pass either `resource` or the combination of `bucket` and `object`.  If you pass
both, the logic will use what you pass for `resource`.

Today it is not possible to pass canonicalized extension headers.

The output of the callout is a set of context variables:

| name                     | meaning                                                                            |
| ------------------------ | ---------------------------------------------------------------------------------- |
| sign\_signedurl           | the full signedurl                                                                 |
| sign\_signature           | the base64-encoded, then url-encoded signature value                               |
| sign\_signature_unencoded | the base64-encoded signature value                                                 |
| sign\_expiration          | The expiration value, in seconds-since-epoch. Computed from NOW + expires-in.      |
| sign\_duration            | The duration, (expiration time - now), in seconds. For diagnostic information.     |
| sign\_expiration\_ISO      | An ISO-formatted string for the expiration. For diagnostics and human consumption. |


### V2 Example
```
ORG=myorg
ENV=myenv
endpoint=https://$ORG-$ENV.apigee.net
curl -i $endpoint/signurl/v2/example
```

You should see a signature upon output:

```
HTTP/1.1 200 OK
Date: Fri, 25 Oct 2019 02:34:52 GMT
Content-Type: application/json
Content-Length: 1125
Connection: keep-alive

{
  "status" : "ok",
  "resource" : "/example-bucket/cat-pics/tabby.png",
  "signature" : "SDdtBiLmQpxWCdZnc3dhtG3fnrnWkMTrdqcn1xd0SYrehUjNLmUakSSRvFAsh5ZWPnyvRqPgExkBYM%2BjjhRAtVQuX6NinF9gtGfNDEk2uH%2BpIAhu%2BnPNlqEtanO7CGM7ARn%2FuSsWmVib%2FY2ihF%2FVtjNzfQyXd3Z%2B4xFkxIpz2SnKTBwcxS%2BGOw7FB5aUiKm3Y6%2FtfFiByDWcJFNZoUTMCMdHrZgovClMAXGVDnFeH99ud9jMJLtv5m8nefrLEizFqlk%2B5T4aMbiOzSdZXAqrGrfzhHiK53KR59eTdGdmSrDNrhLKXe6FhBmyw9BefNSFQie9dmxqv3jEx5uqVObxkA%3D%3D",
  "expiration" : {
    "seconds" : 1571971492,
    "relative" : 600,
    "ISO" : "2019-10-25T02:44:52Z"
  },
  "signedurl" : "https://storage.googleapis.com/example-bucket/cat-pics/tabby.png?GoogleAccessId=account-223456789@project-apigee.iam.gserviceaccount.com&Expires=1571971492&Signature=SDdtBiLmQpxWCdZnc3dhtG3fnrnWkMTrdqcn1xd0SYrehUjNLmUakSSRvFAsh5ZWPnyvRqPgExkBYM%2BjjhRAtVQuX6NinF9gtGfNDEk2uH%2BpIAhu%2BnPNlqEtanO7CGM7ARn%2FuSsWmVib%2FY2ihF%2FVtjNzfQyXd3Z%2B4xFkxIpz2SnKTBwcxS%2BGOw7FB5aUiKm3Y6%2FtfFiByDWcJFNZoUTMCMdHrZgovClMAXGVDnFeH99ud9jMJLtv5m8nefrLEizFqlk%2B5T4aMbiOzSdZXAqrGrfzhHiK53KR59eTdGdmSrDNrhLKXe6FhBmyw9BefNSFQie9dmxqv3jEx5uqVObxkA%3D%3D"
}

```

As with the V4 "sample" signature above, this signature is the right format, but won't be valid. That's because the
bucket name and object name are fake, and the key used to sign is not registered
with GCS. They are sample data.

To actually produce a valid signed url, provide all the necessary inputs explicitly:

```
curl -i $endpoint/signurl/v2/real \
  -X POST \
  -H content-type:application/x-www-form-urlencoded \
  -d bucket=my-gcs-bucket-name \
  -d object=my-gcs-object-name
```

The resulting URL will be usable in a browser.


## Bugs

* The V2 callout does not support producing signed URLs that require additional
  headers. This is a "won't fix" bug because V2 signing is deprecated.
