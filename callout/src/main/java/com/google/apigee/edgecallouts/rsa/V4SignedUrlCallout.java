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

package com.google.apigee.edgecallouts.rsa;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.IOIntensive;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.bouncycastle.crypto.digests.SHA256Digest;

@IOIntensive
public class V4SignedUrlCallout extends SigningCalloutBase implements Execution {
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

  private static final String V4_SIGNED_URL_SPEC =
      "https://storage.googleapis.com{sign_resource}?{sign_canonical_query_string}&X-Goog-Signature={sign_signature}";

  public V4SignedUrlCallout(Map properties) {
    super(properties);
  }

  private String getHashedCanonicalRequest(final MessageContext msgCtxt) throws Exception {
    // CanonicalRequest =
    //   HTTP_VERB + "\n" +
    //   PATH_TO_RESOURCE + "\n" +
    //   CANONICAL_QUERY_STRING + "\n" +
    //   CANONICAL_HEADERS + "\n" +
    //   "\n" +
    //   SIGNED_HEADERS + "\n" +
    //   PAYLOAD

    String verb = getSimpleRequiredProperty("verb", msgCtxt);
    String resource = getSimpleRequiredProperty("resource", msgCtxt);

    String canonicalRequest = verb + "\n" + resource + "\n" + "";

    // TODO: implement the rest of this

    SHA256Digest digest = new SHA256Digest();
    byte[] messageBytes = canonicalRequest.getBytes(StandardCharsets.UTF_8);
    byte[] output = new byte[digest.getDigestSize()];
    digest.update(messageBytes, 0, messageBytes.length);
    digest.doFinal(output, 0);
    return org.bouncycastle.util.encoders.Hex.toHexString(output);
  }

  private String getSigningBase(final MessageContext msgCtxt) throws Exception {
    // StringToSign =
    //   SIGNING_ALGORITHM + "\n" +
    //   CURRENT_DATETIME + "\n" +
    //   CREDENTIAL_SCOPE + "\n" +
    //   HASHED_CANONICAL_REQUEST
    final String signingAlgorithm = "GOOG4-RSA-SHA256";

    // CURRENT_DATETIME, in the ISO 8601 basic format, eg 20181026T211942Z
    final String currentTime =
        ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(formatter);

    // CREDENTIAL SCOPE: The credential scope of the request for signing the string-to-sign.
    String credentialScope = "foo";
    // HASHED_CANONICAL_REQUEST: The hex-encoded, SHA-256 hash of the canonical request, which you
    // created in the previous step.

    String stringToSign =
        signingAlgorithm
            + "\n"
            + currentTime
            + "\n"
            + credentialScope
            + "\n"
            + getHashedCanonicalRequest(msgCtxt);

    msgCtxt.setVariable(varName("signing_string"), stringToSign);
    return stringToSign;
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String signingBase = getSigningBase(msgCtxt);
      KeyPair keypair = getPrivateKey(msgCtxt);
      byte[] signatureBytes = sign_RSA_SHA256(signingBase, keypair);
      String signatureVar = varName("signature");
      String hexSignature = org.bouncycastle.util.encoders.Hex.toHexString(signatureBytes);
      msgCtxt.setVariable(signatureVar, hexSignature);
      msgCtxt.setVariable(varName("signedurl"), resolvePropertyValue(V4_SIGNED_URL_SPEC, msgCtxt));
      return ExecutionResult.SUCCESS;
    } catch (IllegalStateException exc1) {
      setExceptionVariables(exc1, msgCtxt);
      return ExecutionResult.ABORT;
    } catch (Exception e) {
      // if (getDebug()) {
      //     System.out.println(ExceptionUtils.getStackTrace(e));
      // }
      setExceptionVariables(e, msgCtxt);
      msgCtxt.setVariable(varName("stacktrace"), exceptionStackTrace(e));
      return ExecutionResult.ABORT;
    }
  }
}
