# Signed URL Generator

This is a simple callout that geenrates a signed URL for Google Cloud Storage.

Google Cloud Storage allows apps to create [signed URLs](https://cloud.google.com/storage/docs/access-control/signed-urls) with expiry, to allow third parties a time-limited access to a resource.

The summary is:

Build a string like this:

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

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is copyright 2018, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source but you don't need to compile it in order to use it.


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
  <ResourceURL>java://edge-google-signed-url-1.0.1.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the signature.

| name                 | required | meaning |
| -------------------- | -------- | -------------------------------------------------- |
| private-key          | required | a PEM-encoded string containing an RSA private key |
| private-key-password | optional | the plaintext password for the key, if any. |
| verb                 | required | the verb: GET, POST, etc |
| resource             | required | the resource string, eg: /example-bucket/cat-pics/tabby.jpeg |
| expires-in           | required | a string representing expiry.  10s, 5m, 3d.  With no character suffix, interpreted as "Seconds" |
| content-md5          | optional | the MD5 checksum the client must pass |
| content-type         | optional | content-type header, as above. |

Today it is not possible to pass canonicalized extension headers.

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
Date: Fri, 10 Aug 2018 21:22:45 GMT
Content-Type: application/json
Content-Length: 419
Connection: keep-alive

{
    "status" : "ok",
    "signature" "B9%2F9RG1qERiGSHugt%2B8D4FKNoYzy94P9Tj7f8yP3Bxt1DaQSN7YkKpKrL%2FZmiroskNrPKxf%2BBjB%2BCe0byyRsWtU70pVvzTIvxu0xb5j9MgD5UWJnX12c0lO6VMukVSM%2BCm%2B29%2FE%2BvA5clYLyhRqaYAYTdS4kC%2BUcqZD50UStHGk1PV8ini35ja%2BpT6SSS2h7ZVg1vz5o22jKgaM%2BvdwdO9eXWoA2e%2BbQk8l8DsKUz0pTwndtNOXXorEJ5ZC4c3Rw5Z6j5zqzxDv4lhaPri7byJi7s%2FiIBh46q9HUS2MQhezah1IntuenkdcY8LDfJyD46QJdwQ6sE27rwer15lU0cg%3D%3D"
}

```



## Status

This is a community supported project. There is no warranty for this code.
If you have problems or questions, as on [commmunity.apigee.com](https://community.apigee.com).
