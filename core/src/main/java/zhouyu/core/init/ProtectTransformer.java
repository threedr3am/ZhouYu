package zhouyu.core.init;

import java.io.ByteArrayInputStream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import zhouyu.core.transformer.Transformer;

public class ProtectTransformer implements Transformer {

    @Override
    public boolean condition(String className) {
        return false;
    }

    @Override
    public byte[] transformer(ClassLoader loader, String className, byte[] codeBytes) {
        return check(className, loader, codeBytes);
    }

    private byte[] check(String className, ClassLoader loader, byte[] codeBytes) {
        CtClass ctClass = null;
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(new LoaderClassPath(loader));
            ctClass = classPool.makeClass(new ByteArrayInputStream(codeBytes));
            CtClass[] interfaces = ctClass.getInterfaces();
            if (interfaces != null) {
                for (CtClass anInterface : interfaces) {
                    //遇到其它的agent，直接干掉它，不让它加载
                    if (anInterface.getName().equals("java.lang.instrument.ClassFileTransformer")) {
                        System.out.println(String.format("[ZhouYu] 有新的agent: %s 加载，把它干掉！", className));
                        return new byte[0];
                    }
                }
            }
            return ctClass.toBytecode();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
        return codeBytes;
    }
}
