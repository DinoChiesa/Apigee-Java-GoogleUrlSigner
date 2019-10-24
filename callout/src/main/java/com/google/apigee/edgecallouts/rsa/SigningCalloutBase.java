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

import com.apigee.flow.message.MessageContext;
import com.google.apigee.time.TimeResolver;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

public abstract class SigningCalloutBase {
  private static final String _varprefix = "sign_";
  private Map properties; // read-only
  private static final String variableReferencePatternString = "(.*?)\\{([^\\{\\} ]+?)\\}(.*?)";
  private static final Pattern variableReferencePattern =
      Pattern.compile(variableReferencePatternString);
  private static final String commonError = "^(.+?)[:;] (.+)$";
  private static final Pattern commonErrorPattern = Pattern.compile(commonError);

  public SigningCalloutBase(Map properties) {
    this.properties = properties;
  }

  protected static String varName(String s) {
    return _varprefix + s;
  }

  protected static byte[] sign_RSA_SHA256(String signingBase, KeyPair keyPair)
      throws IOException, CryptoException {
    AsymmetricKeyParameter param1 = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
    byte[] messageBytes = signingBase.getBytes(StandardCharsets.UTF_8);
    RSADigestSigner signer = new RSADigestSigner(new SHA256Digest());
    signer.init(true, param1);
    signer.update(messageBytes, 0, messageBytes.length);
    byte[] signature = signer.generateSignature();
    return signature;
  }

  protected static KeyPair produceKeyPair(PrivateKey privateKey)
      throws InvalidKeySpecException, NoSuchAlgorithmException {
    RSAPrivateCrtKey privCrtKey = (RSAPrivateCrtKey) privateKey;
    PublicKey publicKey =
        KeyFactory.getInstance("RSA")
            .generatePublic(
                new RSAPublicKeySpec(
                    ((RSAPrivateKey) privateKey).getPrivateExponent(),
                    privCrtKey.getPublicExponent()));
    return new KeyPair(publicKey, privateKey);
  }

  protected static KeyPair readKeyPair(String privateKeyPemString, String password)
      throws Exception {
    if (password == null) password = "";

    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
    PEMParser pr = new PEMParser(new StringReader(privateKeyPemString));
    Object o = pr.readObject();

    if (o instanceof PrivateKeyInfo) {
      // eg, "openssl genpkey  -algorithm rsa -pkeyopt rsa_keygen_bits:2048 -out keypair.pem"
      PrivateKey privateKey = converter.getPrivateKey((PrivateKeyInfo) o);
      return produceKeyPair(privateKey);
    }

    if (o instanceof PKCS8EncryptedPrivateKeyInfo) {
      // produced by "openssl genpkey" or the series of commands reqd to sign an ec key
      PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) o;
      JceOpenSSLPKCS8DecryptorProviderBuilder decryptorProviderBuilder =
          new JceOpenSSLPKCS8DecryptorProviderBuilder();
      InputDecryptorProvider decryptorProvider =
          decryptorProviderBuilder.build(password.toCharArray());
      PrivateKeyInfo privateKeyInfo =
          pkcs8EncryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);
      PrivateKey privateKey = converter.getPrivateKey(privateKeyInfo);
      return produceKeyPair(privateKey);
    }

    if (o instanceof PEMEncryptedKeyPair) {
      PEMDecryptorProvider decProv =
          new JcePEMDecryptorProviderBuilder().setProvider("BC").build(password.toCharArray());
      return converter.getKeyPair(((PEMEncryptedKeyPair) o).decryptKeyPair(decProv));
    }

    if (o instanceof PEMEncryptedKeyPair) {
      // produced by "openssl genrsa" or "openssl ec -genkey"
      PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) o;
      PEMDecryptorProvider decryptorProvider =
          new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
      return converter.getKeyPair(encryptedKeyPair.decryptKeyPair(decryptorProvider));
    }

    if (o instanceof PEMKeyPair) {
      // eg, "openssl genrsa -out keypair-rsa-2048-unencrypted.pem 2048"
      return converter.getKeyPair((PEMKeyPair) o);
    }

    throw new Exception("unknown object type when decoding private key");
  }

  protected KeyPair getPrivateKey(MessageContext msgCtxt) throws Exception {
    String privateKeyPemString = getSimpleRequiredProperty("private-key", msgCtxt);
    privateKeyPemString = privateKeyPemString.trim();

    // clear any leading whitespace on each line
    privateKeyPemString = privateKeyPemString.replaceAll("([\\r|\\n] +)", "\n");
    String privateKeyPassword = getSimpleOptionalProperty("private-key-password", msgCtxt);
    return readKeyPair(privateKeyPemString, privateKeyPassword);
  }

  protected long getExpiry(final MessageContext msgCtxt) throws Exception {
    String expiresInExpression = getSimpleOptionalProperty("expires-in", msgCtxt);
    if (expiresInExpression == null || expiresInExpression.equals("")) {
      String expiry = getSimpleOptionalProperty("expiry", msgCtxt);
      if (expiry == null || expiry.equals(""))
        throw new IllegalStateException(
            "the configuration must specify one of expiry or expires-in");
      return Long.valueOf(expiry);
    }
    long expirationInSeconds = TimeResolver.getExpiryDate(expiresInExpression).getTime() / 1000;
    return expirationInSeconds;
  }

  protected boolean getDebug() {
    String value = (String) this.properties.get("debug");
    if (value == null) return false;
    if (value.trim().toLowerCase().equals("true")) return true;
    return false;
  }

  protected String getAccessId(MessageContext msgCtxt) throws Exception {
    String accessId = getSimpleOptionalProperty("access-id", msgCtxt);
    return (accessId == null) ? "GOOGLE_ACCESS_STORAGE_ID" : accessId;
  }

  protected String getSimpleOptionalProperty(String propName, MessageContext msgCtxt)
      throws Exception {
    String value = (String) this.properties.get(propName);
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.equals("")) {
      return null;
    }
    value = resolvePropertyValue(value, msgCtxt);
    if (value == null || value.equals("")) {
      return null;
    }
    return value;
  }

  protected String getSimpleRequiredProperty(String propName, MessageContext msgCtxt)
      throws Exception {
    String value = (String) this.properties.get(propName);
    if (value == null) {
      throw new IllegalStateException(propName + " resolves to an empty string");
    }
    value = value.trim();
    if (value.equals("")) {
      throw new IllegalStateException(propName + " resolves to an empty string");
    }
    value = resolvePropertyValue(value, msgCtxt);
    if (value == null || value.equals("")) {
      throw new IllegalStateException(propName + " resolves to an empty string");
    }
    return value;
  }

  // If the value of a property contains any pairs of curlies,
  // eg, {apiproxy.name}, then "resolve" the value by de-referencing
  // the context variables whose names appear between curlies.
  protected static String resolvePropertyValue(String spec, MessageContext msgCtxt) {
    Matcher matcher = variableReferencePattern.matcher(spec);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "");
      sb.append(matcher.group(1));
      Object v = msgCtxt.getVariable(matcher.group(2));
      if (v != null) {
        sb.append((String) v);
      }
      sb.append(matcher.group(3));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  protected void setExceptionVariables(Exception exc1, MessageContext msgCtxt) {
    String error = exc1.toString().replaceAll("\n", " ");
    msgCtxt.setVariable(varName("exception"), error);
    Matcher matcher = commonErrorPattern.matcher(error);
    if (matcher.matches()) {
      msgCtxt.setVariable(varName("error"), matcher.group(2));
    } else {
      msgCtxt.setVariable(varName("error"), error);
    }
  }

  protected static String exceptionStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
