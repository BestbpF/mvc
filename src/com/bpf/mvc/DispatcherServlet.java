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
