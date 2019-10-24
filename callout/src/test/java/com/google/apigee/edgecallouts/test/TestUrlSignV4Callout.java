// Copyright 2018-2019 Google LLC
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

package com.google.apigee.edgecallouts.test;

import com.apigee.flow.execution.ExecutionResult;
import com.google.apigee.edgecallouts.rsa.V4SignedUrlCallout;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestUrlSignV4Callout extends TestSignBase {

  private String serviceAccountKey1 = "{\n"
+"  \"type\": \"service_account\",\n"
+"  \"project_id\": \"project-apigee\",\n"
+"  \"private_key_id\": \"0bb2933e52e4dffa0958ba53ef9226c2a573add1\",\n"
    +"  \"private_key\": \"" + privateKey2.replaceAll("\n", "\\\\n") + "\",\n"
+"  \"client_email\": \"account-223456789@project-apigee.iam.gserviceaccount.com\",\n"
+"  \"client_id\": \"112345888385643765817\",\n"
+"  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
+"  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
+"  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
+"  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/account-223456789%40project-apigee.iam.gserviceaccount.com\"\n"
+"}\n";

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
    props.put("service-account-key", "{\"type\" : \"service_account\", \"client_email\": \"foo\", \"private_key\" : \"this is not a private key\"}");
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
    Assert.assertTrue(Integer.parseInt((String)duration) <= 600);

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    Object signedUrl = msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");

    System.out.println("signedUrl: " + signedUrl);
    System.out.println("signature: " + signature);
    System.out.println("expiry: " + expirationISO);
    System.out.println("duration: " + duration);
    System.out.println("=========================================================");
  }
}
