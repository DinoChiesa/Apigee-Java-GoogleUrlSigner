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

public class TestUrlSignV2Callout extends TestSignBase {

  @Test
  public void emptyVerb() throws Exception {
    String expectedError = "verb resolves to an empty string";
    Map<String, String> props = new HashMap<String, String>();
    // props.put("verb", "GET");
    props.put("expires-in","1d");
    props.put("resource","/foo/bar/bam");
    props.put("service-account-key", serviceAccountKey1);

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

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
  public void missingExpiresIn() throws Exception {
    String expectedError = "the configuration must specify one of expiry or expires-in";

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    // props.put("expires-in","1d");
    props.put("resource","/foo/bar/bam");
    props.put("service-account-key", serviceAccountKey1);

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

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
  public void missingResource() throws Exception {
    String expectedError = "specify either resource or bucket + object";
    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1d");
    // props.put("resource","/foo/bar/bam");
    props.put("service-account-key", serviceAccountKey1);

    V2SignedUrlCallout callout = new V2SignedUrlCallout(props);

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
  public void missingPrivateKey() throws Exception {
    String expectedError = "service-account-key resolves to an empty string";
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
    Assert.assertNull(stacktrace, "stacktrace");
  }

  // @Test
  // public void bogusPrivateKey() throws Exception {
  //   String expectedError = "unknown object type when decoding private key";
  //   // msgCtxt.setVariable("my-private-key", privateKey1);
  //
  //   Map<String, String> props = new HashMap<String, String>();
  //   props.put("verb", "GET");
  //   props.put("expires-in", "1d");
  //   props.put("resource", "/foo/bar/bam");
  //   props.put("private-key", "this-isnot-a-real-private-key");
  //   props.put("private-key-password", "Secret123");
  //
  //   V2SignedUrlCallout callout = new V2SignedUrlCallout(props);
  //
  //   // execute and retrieve output
  //   ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
  //   Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
  //   Object errorOutput = msgCtxt.getVariable("sign_error");
  //   Assert.assertNotNull(errorOutput, "errorOutput");
  //   // System.out.printf("expected error: %s\n", errorOutput);
  //   Assert.assertEquals(errorOutput, expectedError, "error not as expected");
  //   Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
  //   Assert.assertNotNull(stacktrace, "stacktrace");
  // }

  // @Test
  // public void goodResult() throws Exception {
  //   msgCtxt.setVariable("my-private-key", privateKey1);
  //
  //   Map<String, String> props = new HashMap<String, String>();
  //   props.put("verb", "GET");
  //   props.put("expires-in", "1d");
  //   props.put("resource", "/foo/bar/bam");
  //   props.put("access-id", "ABCDEFG123456");
  //   props.put("private-key", "{my-private-key}");
  //   props.put("private-key-password", "Secret123");
  //
  //   V2SignedUrlCallout callout = new V2SignedUrlCallout(props);
  //
  //   // execute and retrieve output
  //   ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
  //   Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
  //   Object errorOutput = msgCtxt.getVariable("sign_error");
  //   Assert.assertNull(errorOutput, "errorOutput");
  //   Object stacktrace = msgCtxt.getVariable("sign_stacktrace");
  //   Assert.assertNull(stacktrace, "BogusPrivateKey() stacktrace");
  //
  //   Object signature = msgCtxt.getVariable("sign_signature");
  //   Assert.assertNotNull(signature, "signature");
  //
  //   Object expiration = msgCtxt.getVariable("sign_expiration");
  //   Assert.assertNotNull(expiration, "expiration");
  //
  //   Object duration = msgCtxt.getVariable("sign_duration");
  //   Assert.assertNotNull(duration, "duration");
  //   Assert.assertEquals(Integer.parseInt((String)duration), 86400);
  //
  //   Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
  //   Assert.assertNotNull(expirationISO, "expiration_ISO");
  //
  //   Object signedUrl = msgCtxt.getVariable("sign_signedurl");
  //   Assert.assertNotNull(signedUrl, "signedUrl");
  //
  //   System.out.println("signedUrl: " + signedUrl);
  //   System.out.println("b64: " + signature);
  //   System.out.println("expiry: " + expirationISO);
  //   System.out.println("duration: " + duration);
  //   System.out.println("=========================================================");
  // }

  @Test
  public void goodResult1() throws Exception {

    Map<String, String> props = new HashMap<String, String>();
    props.put("verb", "GET");
    props.put("expires-in", "1m");
    props.put("resource", "/foo/bar/bam");
    props.put("access-id", "ABCDEFG123456");
    props.put("service-account-key", serviceAccountKey1);

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
    Assert.assertEquals(Integer.parseInt((String)duration), 60);

    Object accessid = msgCtxt.getVariable("sign_accessid");
    Assert.assertNotNull(accessid, "accessid");
    Assert.assertEquals(accessid, "ABCDEFG123456");

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    Object signedUrl = msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");

    System.out.println("signedUrl: " + signedUrl);
    System.out.println("b64: " + signature);
    System.out.println("expiry: " + expirationISO);
    System.out.println("duration: " + duration);
    System.out.println("=========================================================");
  }

  @Test
  public void goodResult2() throws Exception {

    Map<String, String> props = new HashMap<String, String>();
    props.put("service-account-key", getKeyFileContents());
    props.put("verb", "GET");
    props.put("expires-in", "5m");
    props.put("bucket", "images-next2019-hyb211-appspot-com");
    props.put("object", "screenshot-20190927-171326.png");

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
    Assert.assertEquals(Integer.parseInt((String)duration), 300);

    Object accessid = msgCtxt.getVariable("sign_accessid");
    Assert.assertNotNull(accessid, "accessid");
    Assert.assertEquals(accessid, "gcs-viewer@next2019-hyb211.iam.gserviceaccount.com");

    Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
    Assert.assertNotNull(expirationISO, "expiration_ISO");

    Object signedUrl = msgCtxt.getVariable("sign_signedurl");
    Assert.assertNotNull(signedUrl, "signedUrl");

    System.out.printf("\n****\n");
    System.out.printf("b64: %s\n", signature);
    System.out.printf("expiry: %s\n", expirationISO);
    System.out.printf("duration: %s\n\n", duration);
    System.out.printf("signedUrl: %s\n", signedUrl);
    System.out.printf("=========================================================\n");
  }
}
