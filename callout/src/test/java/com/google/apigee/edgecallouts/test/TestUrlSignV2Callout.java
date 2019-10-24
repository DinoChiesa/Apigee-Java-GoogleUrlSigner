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
import com.google.apigee.edgecallouts.rsa.V2SignedUrlCallout;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestUrlSignV2Callout extends TestSignBase {

  @Test
  public void test_EmptyVerb() throws Exception {
    String expectedError = "verb resolves to an empty string";
    Map<String, String> props = new HashMap<String, String>();
    props.put("private-key", "not-a-private-key");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "EmptyVerb() stacktrace");
    System.out.println("=========================================================");
  }

  @Test
  public void test_MissingExpiresIn() throws Exception {
    String expectedError = "the configuration must specify one of expiry or expires-in";
    // msgCtxt.setVariable("variable-name", variableValue);

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    // props.put("expires-in","1d");
    props.put("private-key", "not-a-private-key");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "MissingExpiresIn() stacktrace");
    System.out.println("=========================================================");
  }

  @Test
  public void test_MissingResource() throws Exception {
    String expectedError = "resource resolves to an empty string";
    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1d");
    // props.put("resource","/foo/bar/bam");
    props.put("private-key", "not-a-private-key");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "MissingResource() stacktrace");
    System.out.println("=========================================================");
  }

  @Test
  public void test_MissingPrivateKey() throws Exception {
    String expectedError = "private-key resolves to an empty string";
    // msgCtxt.setVariable("my-private-key", privateKey1);

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1d");
    props.put("resource", "/foo/bar/bam");
    // props.put("private-key", "{my-private-key}");
    // props.put("private-key-password", "Secret123");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "MissingPrivateKey() stacktrace");
    System.out.println("=========================================================");
  }

  @Test
  public void test_BogusPrivateKey() throws Exception {
    String expectedError = "unknown object type when decoding private key";
    // msgCtxt.setVariable("my-private-key", privateKey1);

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1d");
    props.put("resource", "/foo/bar/bam");
    props.put("private-key", "this-isnot-a-real-private-key");
    props.put("private-key-password", "Secret123");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNotNull(stacktrace, "BogusPrivateKey() stacktrace");
    System.out.println("=========================================================");
  }

  @Test
  public void test_GoodResult() throws Exception {
    msgCtxt.setVariable("my-private-key", privateKey1);

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1d");
    props.put("resource", "/foo/bar/bam");
    props.put("access-id", "ABCDEFG123456");
    props.put("private-key", "{my-private-key}");
    props.put("private-key-password", "Secret123");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "BogusPrivateKey() stacktrace");

    Object signature = msgCtxt.getVariable("sign_signature");
    Assert.assertNotNull(signature, "signature");

    Object expiration = msgCtxt.getVariable("sign_expiration");
    Assert.assertNotNull(expiration, "expiration");

    Object duration = msgCtxt.getVariable("sign_duration");
    Assert.assertNotNull(duration, "duration");

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    Object signedUrl = msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");

    System.out.println("signedUrl: " + signedUrl);
    System.out.println("b64: " + signature);
    System.out.println("expiry: " + expirationISO);
    System.out.println("=========================================================");
  }

  @Test
  public void test_GoodResult2() throws Exception {
    msgCtxt.setVariable("my-private-key", privateKey2);

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1d");
    props.put("resource", "/foo/bar/bam");
    props.put("access-id", "ABCDEFG123456");
    props.put("private-key", "{my-private-key}");

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("sign_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
    Assert.assertNull(stacktrace, "BogusPrivateKey() stacktrace");

    Object signature = msgCtxt.getVariable("sign_signature");
    Assert.assertNotNull(signature, "signature");

    Object expiration = msgCtxt.getVariable("sign_expiration");
    Assert.assertNotNull(expiration, "expiration");

    Object duration = msgCtxt.getVariable("sign_duration");
    Assert.assertNotNull(duration, "duration");

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    Object signedUrl = msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");

    System.out.println("signedUrl: " + signedUrl);
    System.out.println("b64: " + signature);
    System.out.println("expiry: " + expirationISO);
    System.out.println("=========================================================");
  }
}
