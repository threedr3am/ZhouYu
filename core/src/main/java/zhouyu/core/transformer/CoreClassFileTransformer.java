package zhouyu.core.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import zhouyu.core.init.ProtectTransformer;
import zhouyu.core.init.WriteShellTransformer;

public class CoreClassFileTransformer implements ClassFileTransformer {

    private Instrumentation inst;

    private static final List<Transformer> transformers = new ArrayList<>();

    static {
        transformers.add(new WriteShellTransformer());
        transformers.add(new ProtectTransformer());
    }

    public CoreClassFileTransformer(Instrumentation inst) {
        this.inst = inst;
    }

    public void retransform() {
        Class[] classes = inst.getAllLoadedClasses();
        if (classes != null) {
            Set<Class> classSet = new HashSet<>();
            for (Class aClass : classes) {
                for (Transformer transformer : transformers) {
                    if (transformer.condition(aClass.getName()) && inst.isModifiableClass(aClass)) {
                        classSet.add(aClass);
                        System.out.println(String.format("[ZhouYu] reload class: %s", aClass.getName()));
                        break;
                    }
                }
            }
            if (!classSet.isEmpty()) {
                try {
                    inst.retransformClasses(classSet.toArray(new Class[classSet.size()]));
                } catch (UnmodifiableClassException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        for (Transformer transformer : transformers) {
            classfileBuffer = transformer.transformer(loader, className, classfileBuffer);
        }
        return classfileBuffer;
    }


}
