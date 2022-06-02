// Copyright © 2018-2021 Google LLC
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
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.BeforeMethod;

public class TestSignBase {
  private static final String testKeyDir = "src/test/resources/keys";

  static {
    java.security.Security.addProvider(new BouncyCastleProvider());
  }

  MessageContext msgCtxt;
  InputStream messageContentStream;
  Message message;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void beforeMethod() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String, Object> variables;

          private Map<String, Object> getVariables() {
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            return variables;
          }

          public void $init() {
            variables = new HashMap<String, Object>();
          }

          @Mock()
          @SuppressWarnings("unchecked")
          public <T> T getVariable(final String name) {
            return (T) getVariables().get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            getVariables().put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (getVariables().containsKey(name)) {
              getVariables().remove(name);
            }
            return true;
          }

          @Mock()
          public Message getMessage() {
            return message;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
          @Mock()
          public InputStream getContentAsStream() {
            // new ByteArrayInputStream(messageContent.getBytes(StandardCharsets.UTF_8));
            return messageContentStream;
          }
        }.getMockInstance();
  }

  // protected static final String privateKey1 =
  //     "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
  //         + "MIIFHzBJBgkqhkiG9w0BBQ0wPDAbBgkqhkiG9w0BBQwwDgQIe1dDIKI2EhwCAggA\n"
  //         + "MB0GCWCGSAFlAwQBAgQQijMNrkSU3jGJLHP90tc81ASCBNATKUMZxgfrCN67P3V6\n"
  //         + "/5iqKfoPcvmV+V1XJT9f/Y3YezMOvE9pAUtLv30N7HBcwadwbqsmfqYh7lVDOvpB\n"
  //         + "nyAayr5U0zZtfHS66XinZdtBc8UbMu2pb6DQ0pzrhG/tmo09QD7JDqs2Lq0Z88a4\n"
  //         + "2H5LbgAJMgpFwGVLPR/ZMmRe5zrsOjfmmVnt10hTarKVnjM/pc0S34TpnLlMKSjR\n"
  //         + "fIsqLFNAg9vZP2WHUChmGUNe9YaNZfe1r6S1TiPc5M0y62H996rYIR8FKxys6lxb\n"
  //         + "s0bFoYd0YWA50hDcXltmwyQPYBBRwUbRjLeQTUcR0W75bh34Ee/K9pqfYtQTf5Tw\n"
  //         + "+DiVv9FgDW9bIi30q1iovh7lboBUSWS2X4dfN1f/CDOFdeEm0Mi6yE/qqGDpjVrF\n"
  //         + "88xpmLnCy4WvKu97f4CLiL5fsVQu3yP9T6aldP+NOq4qXg96kpjwBjQDjCYRMpCi\n"
  //         + "Z8OHhoWa10EzRM8p5e4DiXco5YzVd5CpdxshKxT/sCvpHmWpVjzruANTXNQXXy6N\n"
  //         + "kWO+5PT9nSpb7+GOHruWrImkyytt6Yq53Rli6FCf22cgLxHaIN6mCbQuxb6InVxh\n"
  //         + "h1a7ccvbR2d7rk9FVbrfLSQ5vEWJnYFpoxWvrQGwKQHaYHbYfqH/oouaiN1vDrzu\n"
  //         + "NW0+y+lSYrMy+Rxv+vPD5EBt7aY1tj9sgrcWcHlSpkoyAttmWgmoF5TGF4A8M76r\n"
  //         + "+dzAdkkxqxGUP6prdkGvleWCwRnrmEXyKYILc2MtJxG45bD/XpSQKitkyRnXFF+J\n"
  //         + "MpdYCZES0NgFauPxVgnl4xkKjcpdV6e3HaJHatWY1/D6M1vIH0n/RT8uQhu/YpzF\n"
  //         + "hvsUsc+E6/jCN/P4mN6FlCugBzEouIseRhdXIL9qzQdSE1MmVzERlFNkNeqD+j+I\n"
  //         + "LvktK2s/VhBZxAf2yU9t4a92wQRaQyLPlsB/KFJ8tbGQGpgu1OqiJ4BcKlFBp2Jq\n"
  //         + "p4ivjcD+S4aKzMyQI9fMEyxOrHN0sfAHq2VBDS2QkcYWhe6qlckkDQJ7tWRhqzmi\n"
  //         + "k6LFGnjbA6RPnABJ9N2/JX4bEzlOeODiMXD81FLeHTlNUBgSNx8Itwm3DU6Jnv49\n"
  //         + "PqWICTMHWmXUkAwLbjydRBO6MVUQUNVpcM/dl5M/x0KPsghX7gXoXiPKIe9xrq6w\n"
  //         + "FcXZa2hED/9EJLLz6WvMtqX1BcrxA+wbueiTN8y+1GI6UkvTg42Iw//2t4qKwMv+\n"
  //         + "Q/jadrmxIgcyTe5GVxGUWmC336vW3bz2Vc7IEWDUcX0x+XLaw4ByKbKx2bti1mcN\n"
  //         + "zz/r2GZw1BtdWVCFQw2NfF4rLM5GCbrjF4XG5RB0Lbp1Q2XqXXRKJXR6kZuTgDgU\n"
  //         + "dFwGqhP1mwCGs9/Pg0AfGvqn+jcGipVevx/OFEiu+eK6VNYz4vAt5gU3sLyUwcpC\n"
  //         + "2vUN8Kh4TY4J4oJNeDibWU//qu35c+SoQQPC1L850ZCFsXoGg9TCGuhG97KlZxNw\n"
  //         + "i+CJHKTOWpPwLiPrVPtIp+Q6X8sRibLBdetXhq0P6Nh4mRew6iUg1DzClkK5/jFP\n"
  //         + "Tt5sUjnIV974hjP7F2e64scWXAIoEDYPdhhP/uLbxUmy0Cr9Jt8uUEGb+H7nWOUe\n"
  //         + "1MYSgvlF9eUm6e21FySl6H1kgw==\n"
  //         + "-----END ENCRYPTED PRIVATE KEY-----\n";

  protected static final String privateKey2 =
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

  protected static final String serviceAccountKey1 =
      "{\n"
          + "  \"type\": \"service_account\",\n"
          + "  \"project_id\": \"project-apigee\",\n"
          + "  \"private_key_id\": \"0bb2933e52e4dffa0958ba53ef9226c2a573add1\",\n"
          + "  \"private_key\": \""
          + privateKey2.replaceAll("\n", "\\\\n")
          + "\",\n"
          + "  \"client_email\": \"account-223456789@project-apigee.iam.gserviceaccount.com\",\n"
          + "  \"client_id\": \"112345888385643765817\",\n"
          + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
          + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
          + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
          + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/account-223456789%40project-apigee.iam.gserviceaccount.com\"\n"
          + "}\n";

  protected static String getKeyFileContents(String keyfile) throws Exception {
    Path path = Paths.get(testKeyDir, keyfile);
    if (!Files.exists(path)) {
      return null;
    }
    return new String(Files.readAllBytes(path));
  }

  protected static String getKeyFileContents() throws Exception {
    File testDir = new File(testKeyDir);
    if (!testDir.exists()) {
      throw new IllegalStateException("no test keys directory.");
    }
    String contents = getKeyFileContents("credentials--DO-NOT-COMMIT.json");
    if (contents == null) contents = getKeyFileContents("credentials.json");
    return contents;
  }
}
