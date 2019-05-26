package com.bpf.mvc.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 扫描指定包下的类
 */
public class ClassScanner {

    public static Map<String, Class<?>> scannerClass(String basePackage) {
        Map<String, Class<?>> result = new HashMap<>();
        //通过包名将.替换成/
        String filePath = basePackage.replace(".", "/");
        //通过类加载器获取完整路径
        try {
            //获取字节码文件的位置
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(filePath);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                //判断是否是文件
                if (url.getProtocol().equals("file")) {
                    String realPath = url.getPath().substring(1);
                    File file = new File(realPath);
                    scannerFile(file, realPath.substring(realPath.lastIndexOf(filePath)), result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void scannerFile(File folder, String  realPath, Map<String, Class<?>> classes) {
        File[] files = folder.listFiles();
        for (int i = 0; files != null && i < files.length; i ++) {
            File file = files[i];
            if (file.isDirectory()) {
                //递归查找指定路径下的所有class字节码文件
                scannerFile(file, realPath + file.getName() + "/", classes);
            } else {
                //class文件的绝对路径
                String path = file.getAbsolutePath();
                if (path.endsWith(".class")) {
                    path = path.replace("\\", "/");
                    String className = realPath + path.substring(path.lastIndexOf("/") + 1, path.indexOf(".class"));
                    //获取类名称 eg:com.bpf.mvc.annotation.Controller
                    className = className.replace("/", ".");
                    try {
                        classes.put(className, Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
