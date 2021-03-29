package zhouyu.core.transformer;

public interface Transformer {

    boolean condition(String className);

    byte[] transformer(ClassLoader loader, String className, byte[] codeBytes);
}
