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
| expires-in           | optional | a string representing _relative_ expiry.  10s, 5m, 2h, 3d.  With no character suffix, interpreted as "seconds". This interval is added to the current time to calculate expiry. |
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
| sign_signature           | the base64-encoded signature value                                                 |
| sign_signature_unencoded | the unencoded signature value                                                      |
| sign_expiration          | The expiration value, in seconds-since-epoch. Computed from NOW + expires-in.      |
| sign_duration            | The duration, (expiration time - now), in seconds. For diagnostic information.     |
| sign_expiration_ISO      | An ISO-formatted string for the expiration. For diagnostics and human consumption. |


## Example

See the attached [bundle](./bundle) for a working API Proxy.
To use it, deploy it to any org and environment, then invoke it like this:

```
ORG=myorg
ENV=myenv
curl -i https://$ORG-$ENV.apigee.net/signurl/t1
```

You should see a signature upon output:

```
HTTP/1.1 200 OK
Date: Thu, 01 Aug 2019 17:39:21 GMT
Content-Type: application/json
Content-Length: 1088
Connection: keep-alive

{
  "status" : "ok",
  "resource" : "/example-bucket/cat-pics/tabby.png",
  "signature" : "ycDqbj8X2EaT%2FHHIKc7xVMAsFJDCBhN9B0ME8r2n1czBEAkrmwG081M%2F8V3PB0QeXljdK7n188qpsut8jupogjxYB743L%2FSz%2FOQ%2BT%2BnAtXnKZAYDwcZaMc5zCkXfS4Hj2%2FMIGsS6LtZeB9%2BdtlgSgKk4MVCiOGe19GFJgn0BQnW%2B%2FltpcBKS0yTruPNpNXQZ0LnvARcSq%2BvTQmHDsu9Knu4vw9Qd29ZXg02LvY2kAbqIwf8y3OiW43sFyrmGkeG4v%2F%2FC4QSCgo4OjSL4GaoVzuOeAhdgKgi2KrWcS0xw5WtnTMJMsvJlzMh6%2Bl4QxVBVYiO1BuUxE35NbxgqE3xkxA%3D%3D",
  "expiration" : {
    "seconds" : 1564681761,
    "ISO" : "2019-08-01T17:49:21Z"
  },
  "signedurl" : "https://storage.googleapis.com/example-bucket/cat-pics/tabby.png?GoogleAccessId=GOOGLE_ACCESS_STORAGE_ID&Expires=1564681761&Signature=ycDqbj8X2EaT%2FHHIKc7xVMAsFJDCBhN9B0ME8r2n1czBEAkrmwG081M%2F8V3PB0QeXljdK7n188qpsut8jupogjxYB743L%2FSz%2FOQ%2BT%2BnAtXnKZAYDwcZaMc5zCkXfS4Hj2%2FMIGsS6LtZeB9%2BdtlgSgKk4MVCiOGe19GFJgn0BQnW%2B%2FltpcBKS0yTruPNpNXQZ0LnvARcSq%2BvTQmHDsu9Knu4vw9Qd29ZXg02LvY2kAbqIwf8y3OiW43sFyrmGkeG4v%2F%2FC4QSCgo4OjSL4GaoVzuOeAhdgKgi2KrWcS0xw5WtnTMJMsvJlzMh6%2Bl4QxVBVYiO1BuUxE35NbxgqE3xkxA%3D%3D"
}

```

## Bugs

The V2 callout does not support producing signed URLs that require additional headers.

