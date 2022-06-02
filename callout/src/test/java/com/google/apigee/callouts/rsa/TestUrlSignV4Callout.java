// Copyright 2018-2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts.rsa;

import com.apigee.flow.execution.ExecutionResult;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestUrlSignV4Callout extends TestSignBase {

  @Test
  public void emptyServiceAccountKey() throws Exception {
    String expectedError = "service-account-key resolves to an empty string";
    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");
  }

  @Test
  public void badServiceAccountKey1() throws Exception {
    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", "not-json");
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNotNull(stacktrace, "stacktrace");
  }

  @Test
  public void badServiceAccountKey2() throws Exception {
    String expectedError = "the service account key data is invalid";
    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", "{\"foo\" : \"bar\"}");
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");
  }

  @Test
  public void badServiceAccountKey3() throws Exception {
    String expectedError = "the service account key data is missing the client_email";
    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", "{\"type\" : \"service_account\"}");
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");
  }

  @Test
  public void badServiceAccountKey4() throws Exception {
    String expectedError = "the service account key data is missing the private_key";
    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", "{\"type\" : \"service_account\", \"client_email\": \"foo\"}");
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");
  }

  @Test
  public void badPrivateKey() throws Exception {
    String expectedError = "unknown object type when decoding private key";
    Map<String, String> props = new HashMap<String, String>();
    props.put(
        "service-account-key",
        "{\"type\" : \"service_account\", \"client_email\": \"foo\", \"private_key\" : \"this is not a private key\"}");
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNotNull(stacktrace, "stacktrace");
  }

  @Test
  public void noResource() throws Exception {
    String expectedError = "specify either resource or bucket + object";
    Map<String, String> props = new HashMap<String, String>();
    props.put(
        "service-account-key",
        "{\"type\" : \"service_account\", \"client_email\": \"foo\", \"private_key\" : \"this is not a private key\"}");
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    // props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");
  }

  @Test
  public void goodResult1() throws Exception {
    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", serviceAccountKey1);
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("resource", "/foo/bar");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    Object signature = msgCtxt.getVariable("sign_signature");
    Assert.assertNotNull(signature, "signature");

    Object expiration = msgCtxt.getVariable("sign_expiration");
    Assert.assertNotNull(expiration, "expiration");

    Object duration = msgCtxt.getVariable("sign_duration");
    Assert.assertNotNull(duration, "duration");
    Assert.assertEquals(Integer.parseInt((String) duration), 600);

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    String signedUrl = (String) msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");
    Assert.assertTrue(signedUrl.contains("/foo/bar"));

    System.out.println("signedUrl: " + signedUrl);
    System.out.println("signature: " + signature);
    System.out.println("expiry: " + expirationISO);
    System.out.println("duration: " + duration);
    System.out.println("=========================================================");
  }

  @Test
  public void goodResult2() throws Exception {
    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", getKeyFileContents());
    props.put("verb", "GET");
    props.put("expires-in", "10m");
    props.put("bucket", "images-next2019-hyb211-appspot-com");
    props.put("object", "screenshot-20190927-171326.png");

    V4SignedUrlCallout callout = new V4SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    Object signature = msgCtxt.getVariable("sign_signature");
    Assert.assertNotNull(signature, "signature");

    Object expiration = msgCtxt.getVariable("sign_expiration");
    Assert.assertNotNull(expiration, "expiration");

    Object duration = msgCtxt.getVariable("sign_duration");
    Assert.assertNotNull(duration, "duration");
    Assert.assertEquals(Integer.parseInt((String) duration), 600);

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    String signedUrl = (String) msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");

    System.out.printf("\n****\n");
    System.out.printf(
        "canonical request: %s\n\n", (String) msgCtxt.getVariable("sign_canonical_request"));
    System.out.printf(
        "string-to-sign: %s\n\n", (String) msgCtxt.getVariable("sign_string_to_sign"));
    System.out.printf("signature: %s\n\n", signature);
    System.out.printf("signedUrl: %s\n", signedUrl);
    System.out.printf("expiry: %s\n", expirationISO);
    System.out.printf("duration: %s\n", duration);
    System.out.printf("=========================================================\n");
  }
}
