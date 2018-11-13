# Signed URL Generator

This is a simple callout that geenrates a signed URL for Google Cloud Storage.

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
says the app must construct the final URL to be shared with the aprtner app, like so:

```
FULL_URL =
  BASE_URL +
  "?GoogleAccessId=" + GOOGLE_ACCESS_STORAGE_ID +
  "&Expires=" + EXPIRATION +
  "&Signature=" + URL_ENCODED_SIGNATURE
```

This callout produces only the url-encoded signature, and the expiration. You should rely on a subsequent
AssignMessage/AssignVariable to assemble the full URL. You must know the storage ID, and the base URL.

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
  <ResourceURL>java://edge-google-signed-url-1.0.4.jar</ResourceURL>
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

You must pass one of `expires-in` or `expiry`. If you pass both, `expires-in` takes precedence.

Today it is not possible to pass canonicalized extension headers.

The output of the callout is a set of context variables:

| name                  | meaning |
| --------------------- | ------------------------------------------------------------------------------ |
| sign_output           | the base64-encoded signature value                                                  |
| sign_output_unencoded | the unencoded signature value                                                       |
| sign_expiration       | The expiration value. Computed from NOW + expires-in. You need this for building the URL. |


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
Date: Tue, 13 Nov 2018 02:40:42 GMT
Content-Type: application/json
Content-Length: 1032
Connection: keep-alive

{
  "status" : "ok",
  "resource" : "/example-bucket/cat-pics/tabby.png",
  "signature" : "BFrFC23qUdHaUJ471SUiD6u9dSZZ4yQhB2h3mpukgZvTlZ7a07X7aicEtygE%2FuP%2BJyQYsav%2FJxKMTN6aJpr8%2BEVkVnlqUPncm9Yck%2B9q5BHnn9UgMgHcsrIJee3LifADdMZRGcO0upZ84LQdBISO5O%2FuRTPInGMPjrrAXOJluz4W4SRLPDE3KIwD19SkhROonExj8WMXaujM64ngQhMPGyXb%2FbFUQx6bTeUAVEXzetuqCI73H%2BAOw%2BHyNL%2BXTG4pNI6FCPae4Z%2FNykbL%2Bk8qghQxsvOVnRhfYm5T%2BEzO0Op5yo6ruKKRGbuaHttnlFVOB86vgr0DO6iB%2BqDCHpyF8Q%3D%3D",
  "expiration" : "1542077441",
  "sample_url" : "https://storage.googleapis.com/example-bucket/cat-pics/tabby.png?GoogleAccessId=GOOGLE_ACCESS_STORAGE_ID&Expires=1542077441&Signature=BFrFC23qUdHaUJ471SUiD6u9dSZZ4yQhB2h3mpukgZvTlZ7a07X7aicEtygE%2FuP%2BJyQYsav%2FJxKMTN6aJpr8%2BEVkVnlqUPncm9Yck%2B9q5BHnn9UgMgHcsrIJee3LifADdMZRGcO0upZ84LQdBISO5O%2FuRTPInGMPjrrAXOJluz4W4SRLPDE3KIwD19SkhROonExj8WMXaujM64ngQhMPGyXb%2FbFUQx6bTeUAVEXzetuqCI73H%2BAOw%2BHyNL%2BXTG4pNI6FCPae4Z%2FNykbL%2Bk8qghQxsvOVnRhfYm5T%2BEzO0Op5yo6ruKKRGbuaHttnlFVOB86vgr0DO6iB%2BqDCHpyF8Q%3D%3D"
}

```


## Status

This is a community supported project. There is no warranty for this code.
If you have problems or questions, as on [commmunity.apigee.com](https://community.apigee.com).
