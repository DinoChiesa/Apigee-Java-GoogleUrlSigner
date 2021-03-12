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

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.IOIntensive;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bouncycastle.crypto.digests.SHA256Digest;

@IOIntensive
public class V4SignedUrlCallout extends SigningCalloutBase implements Execution {
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

  private static final String V4_SIGNED_URL_SPEC =
      "https://storage.googleapis.com{sign_resource}?{sign_canonical_query_string}&X-Goog-Signature={sign_signature}";
  private static final String rsaSigningAlgorithm = "GOOG4-RSA-SHA256";

  public V4SignedUrlCallout(Map properties) {
    super(properties);
  }

  private String encodeURIComponent(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private String headersToString(Map<String, String> headers) {
    // TODO: handle the case of a duplicated header name
    return headers.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey().toLowerCase().trim() + ":" + entry.getValue().trim())
        .collect(Collectors.joining("\n"));
  }

  private String queryToString(Map<String, String> query) {
    return query.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + "=" + encodeURIComponent(entry.getValue()))
        .collect(Collectors.joining("&"));
  }

  private Map<String, String> sortMapByKey(Map<String, String> map) {
    return map.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new));
  }

  private Map<String, String> getCanonicalHeaders(final MessageContext msgCtxt) throws Exception {
    Map<String, String> headers = new HashMap<String, String>();
    headers.put("host", "storage.googleapis.com");
    String additionalHeaders = getSimpleOptionalProperty("addl-headers", msgCtxt);
    if (additionalHeaders != null) {
      String[] items = additionalHeaders.split("\\|");
      Arrays.stream(items)
          .forEach(
              item -> {
                if (item != null && !item.equals("")) {
                  String[] kv = item.split(":", 2);
                  if (kv.length == 2
                      && kv[0] != null
                      && !kv[0].equals("")
                      && kv[1] != null
                      && !kv[1].equals("")) {
                    headers.put(kv[0].toLowerCase(), kv[1]);
                  }
                }
              });
    }

    return sortMapByKey(headers);
  }

  private String getCredentialScope(final MessageContext msgCtxt) {
    String nowFormatted = msgCtxt.getVariable(varName("now_formatted"));
    return nowFormatted.substring(0, 8) + "/us/storage/goog4_request";
  }

  private Map<String, String> getCanonicalQuery(
      final MessageContext msgCtxt, String signedHeaders, String serviceAccountEmail)
      throws Exception {
    Map<String, String> query = new HashMap<String, String>();
    query.put("X-Goog-Algorithm", rsaSigningAlgorithm);
    query.put("X-Goog-Credential", serviceAccountEmail + "/" + getCredentialScope(msgCtxt));
    query.put("X-Goog-Date", msgCtxt.getVariable(varName("now_formatted")));
    Instant now = (Instant) msgCtxt.getVariable(varName("now"));
    long expiryEpochSeconds = getExpiry(msgCtxt, now, 604800);
    query.put("X-Goog-Expires", msgCtxt.getVariable(varName("duration")));
    query.put("X-Goog-SignedHeaders", signedHeaders);

    // additional query params
    String additionalQuery = getSimpleOptionalProperty("addl-query", msgCtxt);
    if (additionalQuery != null) {
      String[] items = additionalQuery.split("&");
      Arrays.stream(items)
          .forEach(
              item -> {
                if (item != null && !item.equals("")) {
                  String[] kv = item.split("=", 2);
                  if (kv.length == 2 && !kv[0].equals("") && !kv[1].equals("")) {
                    query.put(kv[0], kv[1]);
                  }
                }
              });
    }
    return sortMapByKey(query);
  }

  private String getHashedCanonicalRequest(
      final MessageContext msgCtxt, final Map<String, String> serviceAccountInfo) throws Exception {
    // CanonicalRequest =
    //   HTTP_VERB + "\n" +
    //   PATH_TO_RESOURCE + "\n" +
    //   CANONICAL_QUERY_STRING + "\n" +
    //   CANONICAL_HEADERS + "\n" +
    //   "\n" +
    //   SIGNED_HEADERS + "\n" +
    //   PAYLOAD

    String clientEmail = serviceAccountInfo.get("client_email");
    if (clientEmail == null)
      throw new IllegalStateException("the service account key data is invalid");

    Map<String, String> canonicalHeaders = getCanonicalHeaders(msgCtxt);
    String signedHeaders =
        canonicalHeaders.keySet().stream()
            .map(e -> e.toLowerCase().trim())
            .collect(Collectors.joining(";"));

    String verb = getSimpleRequiredProperty("verb", msgCtxt);
    String resource = getResource(msgCtxt);
    String canonicalQueryString =
        queryToString(getCanonicalQuery(msgCtxt, signedHeaders, clientEmail));
    msgCtxt.setVariable(varName("canonical_query_string"), canonicalQueryString);
    String canonicalHeadersString = headersToString(canonicalHeaders);
    String payload = getSimpleOptionalProperty("payload", msgCtxt);

    String canonicalRequest =
        verb
            + "\n"
            + resource
            + "\n"
            + canonicalQueryString
            + "\n"
            + canonicalHeadersString
            + "\n"
            + "\n"
            + signedHeaders
            + "\n"
            + (payload != null ? payload : "UNSIGNED-PAYLOAD");

    msgCtxt.setVariable(varName("canonical_request"), canonicalRequest);

    SHA256Digest digest = new SHA256Digest();
    byte[] messageBytes = canonicalRequest.getBytes(StandardCharsets.UTF_8);
    byte[] output = new byte[digest.getDigestSize()];
    digest.update(messageBytes, 0, messageBytes.length);
    digest.doFinal(output, 0);
    return org.bouncycastle.util.encoders.Hex.toHexString(output);
  }

  private String getStringToSign(
      final MessageContext msgCtxt, final Map<String, String> serviceAccountInfo) throws Exception {
    // StringToSign =
    //   SIGNING_ALGORITHM + "\n" +
    //   CURRENT_DATETIME + "\n" +
    //   CREDENTIAL_SCOPE + "\n" +
    //   HASHED_CANONICAL_REQUEST

    String stringToSign =
        rsaSigningAlgorithm
            + "\n"
            + msgCtxt.getVariable(varName("now_formatted"))
            + "\n"
            + getCredentialScope(msgCtxt)
            + "\n"
            + getHashedCanonicalRequest(msgCtxt, serviceAccountInfo);

    msgCtxt.setVariable(varName("string_to_sign"), stringToSign);
    return stringToSign;
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      final Instant now = Instant.now();
      msgCtxt.setVariable(varName("now"), now);
      final String currentTime = ZonedDateTime.ofInstant(now, ZoneOffset.UTC).format(formatter);
      msgCtxt.setVariable(varName("now_formatted"), currentTime);

      Map<String, String> serviceAccountInfo = getServiceAccountKey(msgCtxt);
      String stringToSign = getStringToSign(msgCtxt, serviceAccountInfo);
      KeyPair keypair = readKeyPair(serviceAccountInfo.get("private_key"), null);
      byte[] signatureBytes = sign_RSA_SHA256(stringToSign, keypair);
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
