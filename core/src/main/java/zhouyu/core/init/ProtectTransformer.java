package zhouyu.core.init;

import java.io.ByteArrayInputStream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import zhouyu.core.transformer.Transformer;

public class ProtectTransformer implements Transformer {

    @Override
    public boolean condition(String className) {
        return false;//这里false，意味着，比周瑜这个javaagent更早启动的javaagent，是不会被检测和干掉的！（意味着，正在运行的rasp不会被干掉）
    }

    @Override
    public byte[] transformer(ClassLoader loader, String className, byte[] codeBytes) {
        return check(className, loader, codeBytes);
    }

    private byte[] check(String className, ClassLoader loader, byte[] codeBytes) {
        CtClass ctClass = null;
        try {
            ClassPool classPool = ClassPool.getDefault();
            ctClass = classPool.makeClass(new ByteArrayInputStream(codeBytes));
            if (ctClass != null && check0(className, ctClass)) {
                return new byte[0];
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (ctClass != null) {
                ctClass.detach();
            }
        }
        return codeBytes;
    }

    /**
     * 递归检测java.lang.instrument.ClassFileTransformer接口，防止多层嵌套interface结构绕过
     *
     * @param className
     * @param ctClass
     * @return
     * @throws Throwable
     */
    private boolean check0(String className, CtClass ctClass) throws Throwable {
        CtClass[] interfaces = ctClass.getInterfaces();
        if (interfaces != null) {
            boolean flag = false;
            for (CtClass anInterface : interfaces) {
                //遇到其它的agent，直接干掉它，不让它加载
                if (anInterface.getName().equals("java.lang.instrument.ClassFileTransformer")) {
                    System.out.println(String.format("[ZhouYu] 有新的agent: %s 加载，把它干掉！", className));
                    return true;
                }
                flag |= check0(className, anInterface);
                if (flag) {
                    return flag;
                }
            }
        }
        return false;
    }
}
