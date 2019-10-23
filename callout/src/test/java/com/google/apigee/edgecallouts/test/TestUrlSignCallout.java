package com.google.apigee.edgecallouts.test;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;

import mockit.Mock;
import mockit.MockUp;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.message.Message;

import com.google.apigee.edgecallouts.rsa.SignedUrlCallout;

public class TestUrlSignCallout {

    static {
        java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    MessageContext msgCtxt;
    InputStream messageContentStream;
    Message message;
    ExecutionContext exeCtxt;

    @BeforeMethod()
    public void beforeMethod() {

        msgCtxt = new MockUp<MessageContext>() {
            private Map variables;
            public void $init() {
                variables = new HashMap();
            }

            @Mock()
            public <T> T getVariable(final String name){
                if (variables == null) {
                    variables = new HashMap();
                }
                return (T) variables.get(name);
            }

            @Mock()
            public boolean setVariable(final String name, final Object value) {
                if (variables == null) {
                    variables = new HashMap();
                }
                variables.put(name, value);
                return true;
            }

            @Mock()
            public boolean removeVariable(final String name) {
                if (variables == null) {
                    variables = new HashMap();
                }
                if (variables.containsKey(name)) {
                    variables.remove(name);
                }
                return true;
            }

            @Mock()
            public Message getMessage() {
                return message;
            }
        }.getMockInstance();

        exeCtxt = new MockUp<ExecutionContext>(){ }.getMockInstance();

        message = new MockUp<Message>(){
            @Mock()
            public InputStream getContentAsStream() {
                // new ByteArrayInputStream(messageContent.getBytes(StandardCharsets.UTF_8));
                return messageContentStream;
            }
        }.getMockInstance();
    }

    private static final String privateKey1 =
"-----BEGIN ENCRYPTED PRIVATE KEY-----\n"+
"MIIFHzBJBgkqhkiG9w0BBQ0wPDAbBgkqhkiG9w0BBQwwDgQIe1dDIKI2EhwCAggA\n"+
"MB0GCWCGSAFlAwQBAgQQijMNrkSU3jGJLHP90tc81ASCBNATKUMZxgfrCN67P3V6\n"+
"/5iqKfoPcvmV+V1XJT9f/Y3YezMOvE9pAUtLv30N7HBcwadwbqsmfqYh7lVDOvpB\n"+
"nyAayr5U0zZtfHS66XinZdtBc8UbMu2pb6DQ0pzrhG/tmo09QD7JDqs2Lq0Z88a4\n"+
"2H5LbgAJMgpFwGVLPR/ZMmRe5zrsOjfmmVnt10hTarKVnjM/pc0S34TpnLlMKSjR\n"+
"fIsqLFNAg9vZP2WHUChmGUNe9YaNZfe1r6S1TiPc5M0y62H996rYIR8FKxys6lxb\n"+
"s0bFoYd0YWA50hDcXltmwyQPYBBRwUbRjLeQTUcR0W75bh34Ee/K9pqfYtQTf5Tw\n"+
"+DiVv9FgDW9bIi30q1iovh7lboBUSWS2X4dfN1f/CDOFdeEm0Mi6yE/qqGDpjVrF\n"+
"88xpmLnCy4WvKu97f4CLiL5fsVQu3yP9T6aldP+NOq4qXg96kpjwBjQDjCYRMpCi\n"+
"Z8OHhoWa10EzRM8p5e4DiXco5YzVd5CpdxshKxT/sCvpHmWpVjzruANTXNQXXy6N\n"+
"kWO+5PT9nSpb7+GOHruWrImkyytt6Yq53Rli6FCf22cgLxHaIN6mCbQuxb6InVxh\n"+
"h1a7ccvbR2d7rk9FVbrfLSQ5vEWJnYFpoxWvrQGwKQHaYHbYfqH/oouaiN1vDrzu\n"+
"NW0+y+lSYrMy+Rxv+vPD5EBt7aY1tj9sgrcWcHlSpkoyAttmWgmoF5TGF4A8M76r\n"+
"+dzAdkkxqxGUP6prdkGvleWCwRnrmEXyKYILc2MtJxG45bD/XpSQKitkyRnXFF+J\n"+
"MpdYCZES0NgFauPxVgnl4xkKjcpdV6e3HaJHatWY1/D6M1vIH0n/RT8uQhu/YpzF\n"+
"hvsUsc+E6/jCN/P4mN6FlCugBzEouIseRhdXIL9qzQdSE1MmVzERlFNkNeqD+j+I\n"+
"LvktK2s/VhBZxAf2yU9t4a92wQRaQyLPlsB/KFJ8tbGQGpgu1OqiJ4BcKlFBp2Jq\n"+
"p4ivjcD+S4aKzMyQI9fMEyxOrHN0sfAHq2VBDS2QkcYWhe6qlckkDQJ7tWRhqzmi\n"+
"k6LFGnjbA6RPnABJ9N2/JX4bEzlOeODiMXD81FLeHTlNUBgSNx8Itwm3DU6Jnv49\n"+
"PqWICTMHWmXUkAwLbjydRBO6MVUQUNVpcM/dl5M/x0KPsghX7gXoXiPKIe9xrq6w\n"+
"FcXZa2hED/9EJLLz6WvMtqX1BcrxA+wbueiTN8y+1GI6UkvTg42Iw//2t4qKwMv+\n"+
"Q/jadrmxIgcyTe5GVxGUWmC336vW3bz2Vc7IEWDUcX0x+XLaw4ByKbKx2bti1mcN\n"+
"zz/r2GZw1BtdWVCFQw2NfF4rLM5GCbrjF4XG5RB0Lbp1Q2XqXXRKJXR6kZuTgDgU\n"+
"dFwGqhP1mwCGs9/Pg0AfGvqn+jcGipVevx/OFEiu+eK6VNYz4vAt5gU3sLyUwcpC\n"+
"2vUN8Kh4TY4J4oJNeDibWU//qu35c+SoQQPC1L850ZCFsXoGg9TCGuhG97KlZxNw\n"+
"i+CJHKTOWpPwLiPrVPtIp+Q6X8sRibLBdetXhq0P6Nh4mRew6iUg1DzClkK5/jFP\n"+
"Tt5sUjnIV974hjP7F2e64scWXAIoEDYPdhhP/uLbxUmy0Cr9Jt8uUEGb+H7nWOUe\n"+
"1MYSgvlF9eUm6e21FySl6H1kgw==\n"+
        "-----END ENCRYPTED PRIVATE KEY-----\n";

    private static final String privateKey2 =
            "-----BEGIN PRIVATE KEY-----\n"
                + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCokDY3Bd6pdpVo\n"
                + "c7SaQoOlN2E8KH/zSAfY66fgMk5iNiC90xEnzfEHofCbzT1Pn3euRf0/A6NQNDuQ\n"
                + "m8fRotqrFv4WutMNMVxdlclnrl1xr2VgBHGAQDBqw8iS8F2vSc/ZbZVCA8q2TBK1\n"
                + "mKcyImH8lKVcLPtQsMZWpbwXKSUZpUiTGteEeyMf6GEnwqn7OMWx92xOQZtqpeP+\n"
                + "p7F4dQFwXoZsd7vGFouiP8/bgPuYUcHe5fHi83eiE/5mJPXLlfx8ItW7lJIL68MU\n"
                + "dHZnkjbrJOVw1HAq3biU3KkawTMzkpkgsmUSvcGcADWqRxJfvBlAMendc4ckdpHq\n"
                + "XFqu9iWZAgMBAAECggEBAICZXTNHQCOLe9svgxa5LhRLFty9jTg+uPXue6oY1yIo\n"
                + "Z3xK3ei/PmbzTkyfHWp0n+sOLHH5xYu3/cWKg7zVAPzMUtdmewOyp+QiFYELTvEf\n"
                + "vjityyXsUsPxUEGCLgdASdl4uAmgOPQxP4jZyJ0ADD+V7D5Rdv6NjxOl58THuC1C\n"
                + "ZUq5wyJpm9U+MeUWCYJHWTh3Nj5BVdokYA4G0SeAMuQsGAWXQTR1VTrFEPEouX8a\n"
                + "mCTMYHQP5mfrPD+gAYKGPrjwVyZZI8CnqfxlNhkSt3etuXbHjHHzPb6mPNjOJKgU\n"
                + "5xS5I737wKR1kF0NM14WTeCvSzFNAgo9E9yfVTxIXjECgYEA3rgIgUoA3lk8lsJ/\n"
                + "uOjYRMyDgiYVJ3GMyZ7ll+LqkWRWhEx69NiNeMED32oqKMxBvvM+Q/wgoC5rHJTM\n"
                + "Nd1jbzlqGscJqlW66x8r5bXY9iwhJhiNpNlj+FIPaXktivVG741qTLWnsM5Rrv8L\n"
                + "7leZjEsWNJWAw90FhJTaZ7A3dp0CgYEAwcB+LqjNQQdmNSyBaLTb6Shr6IgOCf/1\n"
                + "NHlqatFsdmy8F2+5+ePExpb7HCbiY5Gi96JBczZ5qEK/yzAIC8WWCLxvtPn/x/vE\n"
                + "ByO7ZXa4dN80KENta0sWWdV3mNFoqU1TR8Cno5a8a3A705CFjI6kSLDxhOSeBfuF\n"
                + "JzErU/oXvC0CgYAF6AeBtj6zptYugVX1x2cE3A+Ywf3Jn/9F0YrxLjleRbTtqUGR\n"
                + "gLSvwR6jLCOWFWSg9b5u+x66YMDCb0fDHe3nIzSnJSQiekeMuLTnUJ1CWgU/B2Oq\n"
                + "PYGjMjnqaCZHCx4oeC2bfy3FSJNt+qGMXpJZ4BvkpRpXF2NwEqqAGXI/GQKBgDam\n"
                + "y3Dx4GO1aJkbIq2cRmOwKTAAIKWlc08H6IKU7BlDdpLNyxG3s6uortA0D6uyStu7\n"
                + "AucyuIJDwcHYnIxlgXqZXJEZ65JHa/XvmE54fHNK+nVY/6ZCGd3hHskWWIVY8GLO\n"
                + "7vpv7FoJ4HY+z8zj92chsh6gNgrN9bMmZWhcpRFJAoGAMih1rmZx8PBwrnkMvwVB\n"
                + "05Ar+LdS/CqG9egQJxtRSIzfdyc9CrZ6b7Sj+VWjieT/o78ODalbXQETia6bYv5b\n"
                + "KWHu/XSeFDzGfCsZiGWECY0rpEKjvI8OBYljTKmB/14Iz51m8jgZRvTaoauUUpZi\n"
                + "w+4PGMrpoKCGFBE4ucT7AvY=\n"
      + "-----END PRIVATE KEY-----\n";

    @Test
    public void test_EmptyVerb() throws Exception {
        String expectedError = "verb resolves to an empty string";
        Map<String,String> props = new HashMap<String,String>();
        props.put("private-key","not-a-private-key");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNotNull(errorOutput, "errorOutput");
        //System.out.printf("expected error: %s\n", errorOutput);
        Assert.assertEquals(errorOutput, expectedError, "error not as expected");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNull(stacktrace, "EmptyVerb() stacktrace");
        System.out.println("=========================================================");
    }

    @Test
    public void test_MissingExpiresIn() throws Exception {
        String expectedError = "the configuration must specify one of expiry or expires-in";
        //msgCtxt.setVariable("variable-name", variableValue);

        Map<String,String> props = new HashMap<String,String>();
        props.put("verb","GET");
        //props.put("expires-in","1d");
        props.put("private-key","not-a-private-key");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNotNull(errorOutput, "errorOutput");
        //System.out.printf("expected error: %s\n", errorOutput);
        Assert.assertEquals(errorOutput, expectedError, "error not as expected");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNull(stacktrace, "MissingExpiresIn() stacktrace");
        System.out.println("=========================================================");
    }

    @Test
    public void test_MissingResource() throws Exception {
        String expectedError = "resource resolves to an empty string";
        Map<String,String> props = new HashMap<String,String>();
        props.put("verb","GET");
        props.put("expires-in","1d");
        //props.put("resource","/foo/bar/bam");
        props.put("private-key","not-a-private-key");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNotNull(errorOutput, "errorOutput");
        //System.out.printf("expected error: %s\n", errorOutput);
        Assert.assertEquals(errorOutput, expectedError, "error not as expected");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNull(stacktrace, "MissingResource() stacktrace");
        System.out.println("=========================================================");
    }


    @Test
    public void test_MissingPrivateKey() throws Exception {
        String expectedError = "private-key resolves to an empty string";
        //msgCtxt.setVariable("my-private-key", privateKey1);

        Map<String,String> props = new HashMap<String,String>();
        props.put("verb","GET");
        props.put("expires-in","1d");
        props.put("resource","/foo/bar/bam");
        //props.put("private-key", "{my-private-key}");
        //props.put("private-key-password", "Secret123");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNotNull(errorOutput, "errorOutput");
        //System.out.printf("expected error: %s\n", errorOutput);
        Assert.assertEquals(errorOutput, expectedError, "error not as expected");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNull(stacktrace, "MissingPrivateKey() stacktrace");
        System.out.println("=========================================================");
    }

    @Test
    public void test_BogusPrivateKey() throws Exception {
        String expectedError = "unknown object type when decoding private key";
        //msgCtxt.setVariable("my-private-key", privateKey1);

        Map<String,String> props = new HashMap<String,String>();
        props.put("verb","GET");
        props.put("expires-in","1d");
        props.put("resource","/foo/bar/bam");
        props.put("private-key", "this-isnot-a-real-private-key");
        props.put("private-key-password", "Secret123");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNotNull(errorOutput, "errorOutput");
        //System.out.printf("expected error: %s\n", errorOutput);
        Assert.assertEquals(errorOutput, expectedError, "error not as expected");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNotNull(stacktrace, "BogusPrivateKey() stacktrace");
        System.out.println("=========================================================");
    }

    @Test
    public void test_GoodResult() throws Exception {
        msgCtxt.setVariable("my-private-key", privateKey1);

        Map<String,String> props = new HashMap<String,String>();
        props.put("verb","GET");
        props.put("expires-in","1d");
        props.put("resource","/foo/bar/bam");
        props.put("access-id","ABCDEFG123456");
        props.put("private-key", "{my-private-key}");
        props.put("private-key-password", "Secret123");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNull(stacktrace, "BogusPrivateKey() stacktrace");

        Object resultB64 = msgCtxt.getVariable("sign_output");
        Assert.assertNotNull(resultB64, "resultB64");

        Object expiration = msgCtxt.getVariable("sign_expiration");
        Assert.assertNotNull(expiration, "expiration");

        Object duration = msgCtxt.getVariable("sign_duration");
        Assert.assertNotNull(duration, "duration");

        Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
        Assert.assertNotNull(expirationISO, "expiration_ISO");

        Object signedUrl = msgCtxt.getVariable("sign_signedurl");
        Assert.assertNotNull(signedUrl, "signedUrl");

        System.out.println("signedUrl: " + signedUrl);
        System.out.println("b64: " + resultB64);
        System.out.println("expiry: " + expirationISO);
        System.out.println("=========================================================");
    }

    @Test
    public void test_GoodResult2() throws Exception {
        msgCtxt.setVariable("my-private-key", privateKey2);

        Map<String,String> props = new HashMap<String,String>();
        props.put("verb","GET");
        props.put("expires-in","1d");
        props.put("resource","/foo/bar/bam");
        props.put("access-id","ABCDEFG123456");
        props.put("private-key", "{my-private-key}");

        SignedUrlCallout callout = new SignedUrlCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("sign_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("sign_stacktrace");
        Assert.assertNull(stacktrace, "BogusPrivateKey() stacktrace");

        Object resultB64 = msgCtxt.getVariable("sign_output");
        Assert.assertNotNull(resultB64, "resultB64");

        Object expiration = msgCtxt.getVariable("sign_expiration");
        Assert.assertNotNull(expiration, "expiration");

        Object duration = msgCtxt.getVariable("sign_duration");
        Assert.assertNotNull(duration, "duration");

        Object expirationISO = msgCtxt.getVariable("sign_expiration_ISO");
        Assert.assertNotNull(expirationISO, "expiration_ISO");

        Object signedUrl = msgCtxt.getVariable("sign_signedurl");
        Assert.assertNotNull(signedUrl, "signedUrl");

        System.out.println("signedUrl: " + signedUrl);
        System.out.println("b64: " + resultB64);
        System.out.println("expiry: " + expirationISO);
        System.out.println("=========================================================");
    }

}
