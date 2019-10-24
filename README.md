# Edge Callout: Signed URL Generator

This is a simple callout that generates a V4 or V2 signed URL for Google Cloud Storage.

Google Cloud Storage allows apps to create [signed
URLs](https://cloud.google.com/storage/docs/access-control/signed-urls) with
expiry, to allow third parties a time-limited access to a resource. As of
September 2019, Google is now recommending the V4 signature method.

This repo includes a class that produces a
[V4 signature](https://cloud.google.com/storage/docs/access-control/signing-urls-manually),
and a separate class that produces a [V2 signature](https://cloud.google.com/storage/docs/access-control/signed-urls-v2).

To use V4 or V2 signed URLs, a client app must build a "String to Sign", and then
sign it with an RSA key, using a SHA256 digest.  Then the app encodes that
signature and embeds the encoded version into a url as a query parameter.

That's what this callout does.

## V2 vs V4

Comparing V2 to V4 signing, the "string to sign" is different, and the signedurl itself is also
different.  Also the V4 signing has more strict limits on the expiry. For more
details see [this link](https://stackoverflow.com/q/58145068/48082).


## Why Signed URLs?

Suppose you need to generate a URL for Google Cloud Storage resource, and expose
it to someone else, to allow that party to access the URL for a given period of
time, with no other authorization. 

You can do that with "signed URLs".  If you want to do this from within an Apigee Edge proxy, this callout might help.

## Why use an Apigee callout rather than direct signing?

Any app that has access to an RSA signing library could implement this signing
itself. In the documentation pages for V4 signing, Google provides examples in
Python and Java, and maybe other languages.

The advantage of using an Apigee callout to perform the signing is that Apigee
can then act as a security mediator. The private key used for signing can remain
secret, held  in Apigee.  Apigee can generate a signed URL and dispense it to a
validated, authenticated client application.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## Status

This is a community supported project. There is no warranty for this code.
If you have problems or questions, as on [commmunity.apigee.com](https://community.apigee.com).

## License

This material is Copyright 2018-2019, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source. And, you don't need to compile it in order to use it.

## Configuration

To use V4 signing, configure the policy like this:

```
<JavaCallout name='Java-URL-Sign-V4'>
  <Properties>
    <Property name='service-account-key'>{my_service_account_json}</Property>
    <Property name='verb'>GET</Property>
    <Property name='resource'>{my_resource}</Property>
    <Property name='expires-in'>10m</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.rsa.V4SignedUrlCallout</ClassName>
  <ResourceURL>java://edge-google-signed-url-20191024.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the signature.

| name                 | required | meaning                                                           |
| -------------------- | -------- | ----------------------------------------------------------------- |
| service-account-key  | required | the contents of the [service account key file](https://cloud.google.com/iam/docs/creating-managing-service-account-keys) from Google. This is a JSON string containing the service account information, includign the private key and client_email.       |
| verb                 | required | the verb: GET, POST, etc                                          |
| resource             | required | the resource string, eg: /example-bucket/cat-pics/tabby.jpeg      |
| expires-in           | optional | a string representing _relative_ expiry.  10s, 5m, 2h, 3d.  With no character suffix, interpreted as "seconds". This interval is added to the current time to calculate expiry. For V4, the longest permitted expiration value is 604800 seconds (7 days). |
| expiry               | optional | a string representing expiry, in absolute seconds-since-epoch.    |
| addl-headers         | optional | a string of name:value pairs, separated by \|  |
| addl-query           | optional | a string of param=value pairs, separated by &  |
| payload              | optional | a string indicating the payload that will be used with the signed request. Empty for GET requests. |

For all properties, you can pass an explicit value or a variable reference,
which is a variable name surrounded by curlies, such
as {verb}.

Pass either `expires-in` or `expiry`. If you pass both, `expires-in` takes precedence.

The output of the callout is a set of context variables:

| name                  | meaning                                                                            |
| --------------------- | ---------------------------------------------------------------------------------- |
| sign_signedurl        | the full signedurl                                                                 |
| sign_signature        | the hex-encoded (aka base16-encoded) signature.                                    |
| sign_expiration       | The expiration value, in seconds-since-epoch. Computed from NOW + expires-in.      |
| sign_duration         | The duration, (expiration time - now), in seconds. For diagnostic information.     |
| sign_expiration_ISO   | An ISO-formatted string for the expiration. For diagnostics and human consumption. |


## Configuration for V2 Signing (DEPRECATED)

To use V2 signing (now deprecated by Google), configure the policy like this:

```
<JavaCallout name='Java-URL-Sign-V2'>
  <Properties>
    <Property name='private-key'>{my_private_key}</Property>
    <Property name='private-key-password'>{my_private_key_password}</Property>
    <Property name='verb'>GET</Property>
    <Property name='resource'>{my_resource}</Property>
    <Property name='expires-in'>10m</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.rsa.V2SignedUrlCallout</ClassName>
  <ResourceURL>java://edge-google-signed-url-20191024.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the signature.

| name                 | required | meaning                                                           |
| -------------------- | -------- | ----------------------------------------------------------------- |
| private-key          | required | a PEM-encoded string containing an RSA private key                |
| private-key-password | optional | the plaintext password for the key, if any.                       |
| verb                 | required | the verb: GET, POST, etc                                          |
| resource             | required | the resource string, eg: /example-bucket/cat-pics/tabby.jpeg      |
| expires-in           | optional | a string representing _relative_ expiry, eg. 10s, 5m, 2h, 3d.  With no character suffix, interpreted as "seconds". |
| expiry               | optional | a string representing expiry, in absolute seconds-since-epoch.    |
| content-md5          | optional | the MD5 checksum the client must pass.                            |
| content-type         | optional | content-type header, as above.                                    |
| access-id            | optional | the GOOGLE_ACCESS_STORAGE_ID. Used for constructing the full URL. |

Pass either `expires-in` or `expiry`. If you pass both, `expires-in` takes precedence.

Today it is not possible to pass canonicalized extension headers.

The output of the callout is a set of context variables:

| name                     | meaning                                                                            |
| ------------------------ | ---------------------------------------------------------------------------------- |
| sign_signedurl           | the full signedurl                                                                 |
| sign_signature           | the base64-encoded, then url-encoded signature value                               |
| sign_signature_unencoded | the base64-encoded signature value                                                 |
| sign_expiration          | The expiration value, in seconds-since-epoch. Computed from NOW + expires-in.      |
| sign_duration            | The duration, (expiration time - now), in seconds. For diagnostic information.     |
| sign_expiration_ISO      | An ISO-formatted string for the expiration. For diagnostics and human consumption. |


## Example

See the attached [bundle](./bundle) for a working API Proxy.
To use it, deploy it to any org and environment, then invoke it like this:

```
ORG=myorg
ENV=myenv
curl -i https://$ORG-$ENV.apigee.net/signurl/v4-t1
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

## Bugs

* The V2 callout does not support producing signed URLs that require additional headers.

