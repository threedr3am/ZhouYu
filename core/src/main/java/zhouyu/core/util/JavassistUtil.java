package zhouyu.core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

public class JavassistUtil {

    public static Set<CtMethod> getAllMethods(CtClass ctClass) {
        Set<CtMethod> ctMethods = new HashSet<>();
        ctMethods.addAll(Arrays.asList(ctClass.getDeclaredMethods()));
        ctMethods.addAll(Arrays.asList(ctClass.getMethods()));
        return ctMethods;
    }

    public static Set<CtConstructor> getAllConstructors(CtClass ctClass) {
        Set<CtConstructor> ctConstructors = new HashSet<>();
        ctConstructors.addAll(Arrays.asList(ctClass.getDeclaredConstructors()));
        ctConstructors.addAll(Arrays.asList(ctClass.getConstructors()));
        return ctConstructors;
    }
}
