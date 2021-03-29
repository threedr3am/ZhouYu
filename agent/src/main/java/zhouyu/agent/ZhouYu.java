package zhouyu.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import zhouyu.core.config.Config;
import zhouyu.core.transformer.CoreClassFileTransformer;

public class ZhouYu {

    public static void premain(String agentArg, Instrumentation inst) {
        init(agentArg, inst);
    }

    public static void agentmain(String agentArg, Instrumentation inst) {
        init(agentArg, inst);
    }

    public static synchronized void init(String action, Instrumentation inst) {
        System.out.println("[ZhouYu] 持久化Agent Shell启动 ...");
        System.out.println(String.format("[ZhouYu] 参数: %s", action));
        try {
            Config.init(action);
            CoreClassFileTransformer coreClassFileTransformer = new CoreClassFileTransformer(inst);
            inst.addTransformer(coreClassFileTransformer, true);
            coreClassFileTransformer.retransform();
        } catch (Throwable e) {
            System.err.println("[ZhouYu] 持久化Agent Shell写入失败！");
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
        throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        if (args.length == 0) {
            System.err.println("[ZhouYu] 参数缺少，例：java -jar ZhouYu.jar 23232，23232为需要attach的jvm进程号！");
            System.exit(-1);
        }
        VirtualMachine vmObj = null;

        try {
            vmObj = VirtualMachine.attach(args[0]);
            String agentpath = ZhouYu.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            if (vmObj != null) {
                if (args.length > 1) {
                    vmObj.loadAgent(agentpath, args[1]);
                } else {
                    vmObj.loadAgent(agentpath);
                }
            }
        } finally {
            if (null != vmObj) {
                vmObj.detach();
            }

        }
    }
}
