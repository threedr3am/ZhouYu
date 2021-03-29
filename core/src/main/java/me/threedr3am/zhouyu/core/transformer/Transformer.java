package me.threedr3am.zhouyu.core.transformer;

/**
 * @author threedr3am
 */
public interface Transformer {

    boolean condition(String className);

    byte[] transformer(ClassLoader loader, String className, byte[] codeBytes);
}
