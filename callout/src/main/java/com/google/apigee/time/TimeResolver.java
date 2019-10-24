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

package com.google.apigee.time;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeResolver {
  private static final Pattern expiryPattern =
      Pattern.compile("^([1-9][0-9]*)(s|m|h|d|w|)$", Pattern.CASE_INSENSITIVE);
  private static final Map<String, Long> timeMultipliers;
  private static String defaultUnit = "s";

  static {
    Map<String, Long> m1 = new HashMap<String, Long>();
    m1.put("s", 1L);
    m1.put("m", 60L);
    m1.put("h", 60L * 60);
    m1.put("d", 60L * 60 * 24);
    m1.put("w", 60L * 60 * 24 * 7);
    timeMultipliers = Collections.unmodifiableMap(m1);
  }

  // public static Date getExpiryDate(String expiresInString) {
  //   Calendar cal = Calendar.getInstance();
  //   Long milliseconds = resolveExpression(expiresInString);
  //   Long seconds = milliseconds / 1000;
  //   int secondsToAdd = seconds.intValue();
  //   if (secondsToAdd <= 0) return null; /* no expiry */
  //   cal.add(Calendar.SECOND, secondsToAdd);
  //   Date then = cal.getTime();
  //   return then;
  // }

  public static Instant getExpiryInstant(String expiresInString) {
    int secondsToAdd = ((Long) resolveExpression(expiresInString)).intValue();
    if (secondsToAdd <= 0) return null; /* no expiry */
    return Instant.now().plusSeconds(secondsToAdd);
  }

  /*
   * convert a simple timespan string, expressed in days, hours, minutes, or
   * seconds, such as 30d, 12d, 8h, 24h, 45m, 30s, into a numeric quantity in
   * seconds. Default TimeUnit is s. Eg. 30 is treated as 30s.
   */
  public static long resolveExpression(String subject) {
    Matcher m = expiryPattern.matcher(subject);
    if (m.find()) {
      String key = m.group(2);
      if (key.equals("")) key = defaultUnit;
      return Long.parseLong(m.group(1), 10) * timeMultipliers.get(key);
    }
    return -1L;
  }
}
