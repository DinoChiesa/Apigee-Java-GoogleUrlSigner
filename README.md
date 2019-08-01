# Edge Callout: Signed URL Generator

This is a simple callout that generates a signed URL for Google Cloud Storage.

Google Cloud Storage allows apps to create [signed URLs](https://cloud.google.com/storage/docs/access-control/signed-urls) with expiry, to allow third parties a time-limited access to a resource.

To use signed URLs, the app must build a string like this:

```
StringToSign = HTTP_Verb + "\n" +
               Content_MD5 + "\n" +
               Content_Type + "\n" +
               Expiration + "\n" +
               Canonicalized_Extension_Headers +
               Canonicalized_Resource
```

Then sign it with an RSA key, using a SHA256 Digest.

That's what this callout does.

[This page](https://cloud.google.com/storage/docs/access-control/create-signed-urls-program)
says the app must construct the final URL to be shared with the partner app, like so:

```
FULL_URL =
  BASE_URL +
  "?GoogleAccessId=" + GOOGLE_ACCESS_STORAGE_ID +
  "&Expires=" + EXPIRATION +
  "&Signature=" + URL_ENCODED_SIGNATURE
```

This callout produces the url-encoded signature, the expiration, and the full signedurl.
It assumes the base URL is https://storage.googleapis.com .

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is copyright 2018, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source. You don't need to compile it in order to use it.


## Configuration

Configure the policy like this:

```
<JavaCallout name='Java-URL-Sign'>
  <Properties>
    <Property name='private-key'>{my_private_key}</Property>
    <Property name='private-key-password'>{my_private_key_password}</Property>
    <Property name='verb'>GET</Property>
    <Property name='resource'>{my_resource}</Property>
    <Property name='expires-in'>10m</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.rsa.SignedUrlCallout</ClassName>
  <ResourceURL>java://edge-google-signed-url-1.0.6.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the signature.

| name                 | required | meaning |
| -------------------- | -------- | -------------------------------------------------- |
| private-key          | required | a PEM-encoded string containing an RSA private key |
| private-key-password | optional | the plaintext password for the key, if any. |
| verb                 | required | the verb: GET, POST, etc |
| resource             | required | the resource string, eg: /example-bucket/cat-pics/tabby.jpeg |
| expires-in           | optional | a string representing _relative_ expiry.  10s, 5m, 2h, 3d.  With no character suffix, interpreted as "seconds". This interval is added to the current time to calculate expiry. |
| expiry               | optional | a string representing expiry, in absolute seconds-since-epoch. |
| content-md5          | optional | the MD5 checksum the client must pass |
| content-type         | optional | content-type header, as above. |
| access-id            | optional | the GOOGLE_ACCESS_STORAGE_ID. Used for constructing the full URL. |

You must pass one of `expires-in` or `expiry`. If you pass both, `expires-in` takes precedence.

Today it is not possible to pass canonicalized extension headers.

The output of the callout is a set of context variables:

| name                  | meaning                                                                            |
| --------------------- | ---------------------------------------------------------------------------------- |
| sign_signedurl        | the full signedurl                                                                 |
| sign_output           | the base64-encoded signature value                                                 |
| sign_output_unencoded | the unencoded signature value                                                      |
| sign_expiration       | The expiration value in seconds. Computed from NOW + expires-in. You need this for building the URL. |
| sign_expiration_ISO   | An ISO-formatted string for the expiration. For diagnostics and human consumption. |


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

This callout does not support producing signed URLs that require additional headers.

## Status

This is a community supported project. There is no warranty for this code.
If you have problems or questions, as on [commmunity.apigee.com](https://community.apigee.com).
