### Spring MVC流程

![Spring MVC流程](https://upload-images.jianshu.io/upload_images/13065313-3a41bc67a2cf8848.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
**步骤**

- 前端控制器DispatcherServlet接收HTTP请求
- 控制器通过处理器映射器HandlerMapping找到并返回执行链HandlerExecutionChain
- 控制器调用处理器适配器HandlerAdapter去执行处理器，执行完成后返回ModelAndView给控制器
- 控制器请求视图解析器去资源定位，视图解析
- 控制器对视图进行数据渲染

### 撸一个简单的Spring MVC框架

`定义两个基本注解-@Controller和@RequestMapping`

```java
package com.bpf.mvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识一个类为控制器
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
}
```

```java
package com.bpf.mvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识控制器或其内部方法的请求映射
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    /**
     * url请求路径
     * @return
     */
    String value();
}
```

`定义一个包扫描类`

```java
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
```

`定义一个DispatcherServlet`

```java
package com.bpf.mvc;

import com.bpf.mvc.annotation.Controller;
import com.bpf.mvc.annotation.RequestMapping;
import com.bpf.mvc.util.ClassScanner;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 前端控制器
 */
@WebServlet(urlPatterns = "*.htm", initParams = @WebInitParam(name = "basePackage", value = "com.bpf.mvc"))
public class DispatcherServlet extends HttpServlet {

    /**
     * 保存类名和对象的映射
     */
    private Map<String, Object> controllers = new HashMap<>();
    /**
     * 保存url和方法的映射
     */
    private Map<String, Method> methods = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        String basePackage = config.getInitParameter("basePackage");
        Map<String, Class<?>> classMap = ClassScanner.scannerClass(basePackage);
        for (Map.Entry<String, Class<?>> kv : classMap.entrySet()) {
            String className = kv.getKey();
            Class<?> clazz = kv.getValue();
            //获取@Controller以及@RequestMapping标记的内容
            if (clazz.isAnnotationPresent(Controller.class)) {
                String path = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping rqm = clazz.getAnnotation(RequestMapping.class);
                    path = rqm.value().startsWith("/") ? rqm.value() : "/" + rqm.value();
                }
                try {
                    controllers.put(className, clazz.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                Method[] ms = clazz.getMethods();
                for (Method m : ms) {
                    if (!m.isAnnotationPresent(RequestMapping.class)) {
                        continue;
                    }
                    String url = m.getAnnotation(RequestMapping.class).value();
                    url = url.startsWith("/") ? url : "/" + url;
                    methods.put(path + url, m);
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String mappingPath = uri.substring(uri.lastIndexOf(contextPath) + contextPath.length());
        Method method = methods.get(mappingPath);
        Object controller = controllers.get(method.getDeclaringClass().getName());
        try {
            method.invoke(controller);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
```

`测试`

```java
package com.bpf.mvc.controller;

import com.bpf.mvc.annotation.Controller;
import com.bpf.mvc.annotation.RequestMapping;

@Controller
public class HelloController {

    @RequestMapping("hello.htm")
    public void hello() {
        System.out.println("hello");//请求接口控制台打印hello
    }

    @RequestMapping("world.htm")
    public void world() {
        System.out.println("world");//请求接口控制台打印world
    }
}
```

### 总结

demo主要实现了通过请求url调用方法的过程，并没有实现视图解析渲染相关操作，有能力者可以自行扩展
