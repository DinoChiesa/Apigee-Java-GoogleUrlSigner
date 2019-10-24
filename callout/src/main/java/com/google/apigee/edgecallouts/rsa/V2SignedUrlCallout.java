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
import java.net.URLEncoder;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.bouncycastle.util.encoders.Base64;

@IOIntensive
public class V2SignedUrlCallout extends SigningCalloutBase implements Execution {
  protected static final String V2_SIGNED_URL_SPEC =
      "https://storage.googleapis.com{sign_resource}?GoogleAccessId={sign_accessid}&Expires={sign_expiration}&Signature={sign_signature}";

  public V2SignedUrlCallout(Map properties) {
    super(properties);
  }

  private String getSigningBase(final MessageContext msgCtxt) throws Exception {

    // StringToSign = HTTP_Verb + "\n" +
    //                Content_MD5 + "\n" +
    //                Content_Type + "\n" +
    //                Expiration + "\n" +
    //                Canonicalized_Extension_Headers +
    //                Canonicalized_Resource

    String verb = getSimpleRequiredProperty("verb", msgCtxt);
    String contentMd5 = getSimpleOptionalProperty("content-md5", msgCtxt);
    String contentType = getSimpleOptionalProperty("content-type", msgCtxt);
    long expirationInSeconds = getExpiry(msgCtxt);
    String resource = getSimpleRequiredProperty("resource", msgCtxt);
    String canonicalizedExtensionHeaders = "";
    String stringToSign =
        verb
            + "\n"
            + (contentMd5 != null ? contentMd5 : "")
            + "\n"
            + (contentType != null ? contentType : "")
            + "\n"
            + expirationInSeconds
            + "\n"
            + canonicalizedExtensionHeaders
            + resource;
    msgCtxt.setVariable(varName("resource"), resource);
    msgCtxt.setVariable(varName("verb"), verb);
    msgCtxt.setVariable(varName("expiration"), Long.toString(expirationInSeconds));
    Instant expiryInstant = Instant.ofEpochSecond(expirationInSeconds);
    Instant now = Instant.now();
    msgCtxt.setVariable(
        varName("duration"), Long.toString(Duration.between(now, expiryInstant).getSeconds()));
    msgCtxt.setVariable(
        varName("expiration_ISO"),
        ZonedDateTime.ofInstant(expiryInstant, ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT));

    msgCtxt.setVariable(varName("signing_string"), stringToSign);
    return stringToSign;
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String signingBase = getSigningBase(msgCtxt);
      KeyPair keypair = getPrivateKey(msgCtxt);
      byte[] resultBytes = sign_RSA_SHA256(signingBase, keypair);
      String signatureVar = varName("signature");
      String signature = Base64.toBase64String(resultBytes);
      msgCtxt.setVariable(signatureVar + "_unencoded", signature);
      msgCtxt.setVariable(signatureVar, URLEncoder.encode(signature, "UTF-8"));
      String accessId = getAccessId(msgCtxt);
      msgCtxt.setVariable(varName("accessid"), accessId);
      msgCtxt.setVariable(varName("signedurl"), resolvePropertyValue(V2_SIGNED_URL_SPEC, msgCtxt));
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
