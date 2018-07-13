package com.spring.bean.servlet;

import com.spring.bean.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by
 *
 * @author Lichensheng
 * @date 2018/7/13 23:38
 * @return
 */
public class DispatchServlet extends HttpServlet {

    private Properties properties = new Properties();
    private List<String> classNameList = new ArrayList<>();
    private Map<String,Object> ioc = new HashMap<String,Object>();
    private Map<String, Method> handleMap = new HashMap<String, Method>();
    private  List<Handler> handlerMapping = new ArrayList<>();
    @Override
    public void init(ServletConfig config) throws ServletException{
        System.out.println("------------init--------");
        super.init(config);
        //加载配置文件
        try {
            doLoadConfig(config.getInitParameter("contextConfigLocation"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //根据配置文件扫描所有相关的类
            doScanner(properties.getProperty("scanPackage"));
        //初始化所有相关的类的实例,并且将其注入到IOC容器中，
            doInstance();
        //实现自动依赖注入
           doAutowired();
        //初始化HandlerMapping
            initHandleMapping();
        //等待请求，进入运行阶段
    }
    private void doLoadConfig(String configLocation) throws IOException{
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(configLocation);
        try {
            properties.load(is);//将配置信息加载到Properties中
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(is != null)
                is.close();
        }
    }

    private void doScanner(String packages) {
       URL url = this.getClass().getClassLoader().getResource("/"+ packages.replaceAll("\\.","/"));
        File classDir = new File(url.getFile());
        for(File file:classDir.listFiles()){
            if(file.isDirectory()){
                doScanner(packages + "." + file.getName());
            }else {
                String className = packages + "." + file.getName().replace(".class","");
                classNameList.add(className);//递归保存类名
            }
        }
    }

    private void doInstance(){
        if(classNameList.isEmpty())
            return;
         try {
             for(String className:classNameList){
                  Class<?> clazz = Class.forName(className);
                  //初始化Ioc容器
                 /**容器规则
                  * key:
                  * 1.默认用类名首字母小写；
                  * 2.如果自定义名字，则优先选择自定义名字；
                  * 3.如果是接口，用接口的类型作为key
                  */
                 //case:1
                 if (clazz.isAnnotationPresent(com.spring.bean.annotation.Controller.class)) {
                        String beanName = lowerFirstCast(clazz.getSimpleName());
                        ioc.put(beanName,clazz.newInstance());

                 }else if(clazz.isAnnotationPresent(com.spring.bean.annotation.Service.class)){
                        //case:2
                        Service service = clazz.getAnnotation(Service.class);
                        String beanName = service.value();
                        if("".equals(beanName.trim())){
                           beanName = lowerFirstCast(clazz.getSimpleName());
                        }
                        Object instance = clazz.newInstance();
                        ioc.put(beanName,instance);
                        //case 3:
                        Class<?>[] interfaces = clazz.getInterfaces();
                        for(Class c : interfaces){
                            //将接口的类型作为key
                            ioc.put(c.getName(),instance);
                        }
                 }else{
                     continue;
                 }
             }
         }catch (Exception e){

         }

    }
    public void doAutowired(){
        if(ioc.isEmpty())
            return;
        for(Map.Entry<String, Object> entry: ioc.entrySet()){
            /**
             * 获取所有的字段Field
             */
           Field[] fields =  entry.getValue().getClass().getDeclaredFields();
           for(Field field:fields){
               if(!field.isAnnotationPresent(Autowired.class)){
                   continue;
               }
               Autowired autowired = field.getAnnotation(Autowired.class);
               String beanName = autowired.value().trim();
               if ("".equals(beanName)) {
                   beanName = field.getType().getName();
               }
               //访问私有的或者受保护的，强制访问赋值
               field.setAccessible(true);
               try {
                   field.set(entry.getValue(),ioc.get(beanName));
               } catch (IllegalAccessException e) {
                   e.printStackTrace();
               }
           }
        }
    }

    /**
     * 获取方法上的URl
     */
    private void initHandleMapping(){
         if(ioc.isEmpty())
             return;
         for(Map.Entry entry:ioc.entrySet()) {
             Class<?> clazz = entry.getValue().getClass();
             if(!clazz.isAnnotationPresent(RequestMapping.class))
                 continue;
             RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
             String baseUrl = requestMapping.value();
             Method[] methods = clazz.getDeclaredMethods();
             for(Method method : methods){
                 String regex = ("/" + baseUrl + requestMapping.value()).replaceAll("/+","/");
                 Pattern pattern = Pattern.compile(regex);
                 handlerMapping.add(new Handler(pattern,entry.getValue(),method));
             }
         }
    }
    private String lowerFirstCast(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return  String.valueOf(chars);
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request,response);
    }

    protected  void doPost(HttpServletRequest request,HttpServletResponse response){
        try {
            doDispatch(request,response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * 反射需要的参数
         * 1、方法对象实例
         * 2、参数值
         * mothod.invoke()
         */

    }
    private void doDispatch(HttpServletRequest request,HttpServletResponse response) throws Exception{
        try {
            Handler handler = getHandler(request);
            if(handler == null){
                response.getWriter().write("404 ot Found");
                return;
            }
            //获取方法参数列表
            Class<?>[] paramType = handler.method.getParameterTypes();
            //保存所有参数
            Object[] paramValues = new Object[paramType.length];
            Map<String,String[]> params = request.getParameterMap();
            for(Map.Entry<String,String[]> param : params.entrySet()){
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
                        .replaceAll(",\\s",",");
                if(!handler.paramIndexMapping.containsKey(param.getKey()))
                    continue;
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramType[index],value);
            }
            int reqIndex= handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;
            handler.method.invoke(handler.controller,paramValues);
        }catch (Exception e){
            throw e;
        }
    }
    private Handler getHandler(HttpServletRequest request){
        if(handlerMapping.isEmpty())
            return null;
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replace(contextPath,"").replace("/+","/");
        for(Handler handler : handlerMapping){
            try {
                Matcher matcher = handler.pattern.matcher(url);
                //如果没有匹配上继续匹配下一个
                if(!matcher.matches())
                    continue;
                return handler;
            }catch (Exception e){
                throw  e;
            }
        }
        return  null;
    }
    //将字符串按照类型进行转换
    public Object convert(Class<?> type , String value){
        if(Integer.class == type ){
            return Integer.valueOf(value);
        }
        return value;
    }
    private class  Handler{
        protected Object controller;//保存方法对应的实例
        protected Method method;//保存映射的方法
        protected Pattern pattern;
        protected  Map<String,Integer> paramIndexMapping;//参数顺序
        protected  Handler(Pattern pattern,Object controller,Method method){
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;

            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }
        private void putParamIndexMapping(Method method){
           //获取方法中加了注解的参数
            Annotation[][] pa = method.getParameterAnnotations();
            for(int i=0;i<pa.length;i++){
                for(Annotation a: pa[i]){
                    if(a instanceof RequestParam){
                        String paramname = ((RequestParam) a).value();
                        if(!"".equals(paramname.trim())){
                            paramIndexMapping.put(paramname,i);
                        }
                    }
                }
            }

            //提取方法中request和response参数
            Class<?> []paramTypes = method.getParameterTypes();
            for(int i=0;i<paramTypes.length;i++){
                Class<?> type = paramTypes[i];
                if(type == HttpServletRequest.class||type == HttpServletResponse.class)
                    paramIndexMapping.put(type.getName(),i);
            }
        }
    }
}
