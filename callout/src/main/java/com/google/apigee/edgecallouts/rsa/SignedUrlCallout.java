package com.google.apigee.edgecallouts.rsa;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.IOIntensive;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.edgecallouts.SigningCalloutBase;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
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
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

@IOIntensive
public class SignedUrlCallout extends SigningCalloutBase implements Execution {
    public SignedUrlCallout (Map properties) {
        super(properties);
    }

    private static byte[] sign_RSA_SHA256(String signingBase, KeyPair keyPair)
        throws IOException,  org.bouncycastle.crypto.CryptoException
    {
        AsymmetricKeyParameter param1 = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        byte[] messageBytes = signingBase.getBytes(StandardCharsets.UTF_8);
        RSADigestSigner signer = new RSADigestSigner(new SHA256Digest());
        signer.init(true, param1);
        signer.update(messageBytes, 0, messageBytes.length);
        byte[] signature = signer.generateSignature();
        return signature;
    }


    private static KeyPair readKeyPair(String privateKeyPemString, String password)
        throws IOException, OperatorCreationException, PKCSException, InvalidKeySpecException, NoSuchAlgorithmException
    {
        if (password == null) password = "";

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PEMParser pr = new PEMParser(new StringReader(privateKeyPemString));
        Object o = pr.readObject();

        if (o == null || !((o instanceof PEMKeyPair) || (o instanceof PEMEncryptedKeyPair) || (o instanceof PKCS8EncryptedPrivateKeyInfo)) ) {
            //System.out.printf("found %s\n", o.getClass().getName());
            throw new IllegalStateException("Didn't find OpenSSL key");
        }

        if (o instanceof PKCS8EncryptedPrivateKeyInfo) {
            // produced by "openssl genpkey" or the series of commands reqd to sign an ec key
            //LOGGER.info("decodePrivateKey, encrypted PrivateKeyInfo");
            PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) o;
            JceOpenSSLPKCS8DecryptorProviderBuilder decryptorProviderBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder();
            InputDecryptorProvider decryptorProvider = decryptorProviderBuilder.build(password.toCharArray());
            PrivateKeyInfo privateKeyInfo = pkcs8EncryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);
            PrivateKey privateKey = converter.getPrivateKey(privateKeyInfo);

            BigInteger publicExponent = BigInteger.valueOf(65537);
            PublicKey publicKey = KeyFactory
                .getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(((RSAPrivateKey)privateKey).getPrivateExponent(), publicExponent));
            return new KeyPair(publicKey, privateKey);
        }

        KeyPair kp;
        if (o instanceof PEMEncryptedKeyPair) {
            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().setProvider("BC")
                .build(password.toCharArray());
            return converter.getKeyPair(((PEMEncryptedKeyPair)o).decryptKeyPair(decProv));
        }

        return converter.getKeyPair((PEMKeyPair)o);
    }

    private KeyPair getPrivateKey(MessageContext msgCtxt) throws Exception {
        String privateKeyPemString = getSimpleRequiredProperty("private-key", msgCtxt);
        privateKeyPemString = privateKeyPemString.trim();

        // clear any leading whitespace on each line
        privateKeyPemString = privateKeyPemString.replaceAll("([\\r|\\n] +)","\n");
        String privateKeyPassword = getSimpleOptionalProperty("private-key-password", msgCtxt);
        return readKeyPair(privateKeyPemString, privateKeyPassword);
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
        String expiryExpression = getSimpleRequiredProperty("expires-in", msgCtxt);
        long expirationInSeconds = TimeResolver.getExpiryDate(expiryExpression).getTime()/1000;
        String resource = getSimpleRequiredProperty("resource", msgCtxt);
        String canonicalizedExtensionHeaders = "";
        String stringToSign = verb + "\n" +
            (contentMd5!=null ? contentMd5 : "") + "\n" +
            (contentType!=null ? contentType : "") + "\n" +
            expirationInSeconds + "\n" +
            canonicalizedExtensionHeaders + 
            resource;
        msgCtxt.setVariable(varName("expiration"), expirationInSeconds+"");
        msgCtxt.setVariable(varName("signing_string"), stringToSign);
        return stringToSign;
    }
    
    static class TimeResolver {
        private final static Pattern expiryPattern =
            Pattern.compile("^([1-9][0-9]*)(ms|s|m|h|d|w|)$", Pattern.CASE_INSENSITIVE);
        private final static Map<String,Long> timeMultipliers;
        private static String defaultUnit = "ms";
        static {
            Map<String,Long> m1 = new HashMap<String,Long>();
            m1.put("ms",1L);
            m1.put("s", 1L*1000);
            m1.put("m", 60L*1000);
            m1.put("h", 60L*60*1000);
            m1.put("d", 60L*60*24*1000);
            m1.put("w", 60L*60*24*7*1000);
            //m1.put("y", 60*60*24*365*1000);
            timeMultipliers = m1;
        }

        public static Date getExpiryDate(String expiresInString) {
            Calendar cal = Calendar.getInstance();
            Long milliseconds = resolveExpression(expiresInString);
            Long seconds = milliseconds/1000;
            int secondsToAdd = seconds.intValue();
            if (secondsToAdd<= 0) return null; /* no expiry */
            cal.add(Calendar.SECOND, secondsToAdd);
            Date then = cal.getTime();
            return then;
        }

        /*
         * convert a simple timespan string, expressed in days, hours, minutes, or
         * seconds, such as 30d, 12d, 8h, 24h, 45m, 30s, into a numeric quantity in
         * seconds. Default TimeUnit is ms. Eg. 30 is treated as 30ms.
         */
        public static Long resolveExpression(String subject) {
            Matcher m = expiryPattern.matcher(subject);
            if (m.find()) {
                String key = m.group(2);
                if(key.equals(""))
                    key = defaultUnit;
                return Long.parseLong(m.group(1),10) * timeMultipliers.get(key);
            }
            return -1L;
        }
    }
    
    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        try {
            String signingBase = getSigningBase(msgCtxt);
            KeyPair keypair = getPrivateKey(msgCtxt);
            byte[] resultBytes = sign_RSA_SHA256(signingBase, keypair);
            String outputVar = getOutputVar(msgCtxt);
            String signature = Base64.toBase64String(resultBytes);
            msgCtxt.setVariable(outputVar+ "_unencoded", signature);
            msgCtxt.setVariable(outputVar, URLEncoder.encode(signature, "UTF-8"));
            return ExecutionResult.SUCCESS;
        }
        catch (IllegalStateException exc1) {
            setExceptionVariables(exc1,msgCtxt);
            return ExecutionResult.ABORT;
        }
        catch (Exception e) {
            if (getDebug()) {
                System.out.println(ExceptionUtils.getStackTrace(e));
            }
            setExceptionVariables(e,msgCtxt);
            msgCtxt.setVariable(varName("stacktrace"), ExceptionUtils.getStackTrace(e));
            return ExecutionResult.ABORT;
        }
    }

}
