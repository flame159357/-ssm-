package com.itheima.ssm.controller;

import com.itheima.ssm.domain.SysLog;
import com.itheima.ssm.service.ISysLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;

//LogAop。java
@Component
@Aspect
public class LogAop {

    @Autowired
    private HttpServletRequest request; //得到request,web.xml中配置监听器

    @Autowired
    private ISysLogService sysLogService;

    private Date visitTime; //开始时间
    private Class clazz;    //访问的类
    private Method method; //访问的方法

    //前置通知 主要是获取开始时间，执行的类是哪一个，访问的是哪一个方法
    @Before("execution(* com.itheima.ssm.controller.*.*(..))")
    public void doBefore(JoinPoint jp) throws NoSuchMethodException {
        visitTime = new Date();  //当前时间就是开始访问的时间
        clazz = jp.getTarget().getClass(); //具体要访问的类
        String methodName = jp.getSignature().getName(); //获取访问的方法的名称
        Object[] args = jp.getArgs();//获取访问方法的参数

        //获取具体执行的方法放入method对象
        if(args == null || args.length == 0){
            //无参
            method = clazz.getMethod(methodName); //根据方法名获取方法对象，只能获取无参数的方法
        }else {
            Class[] classArgs = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                classArgs[i] = args[i].getClass(); //获取每一个参数的class存储到数组中
            }
            method = clazz.getMethod(methodName,classArgs);
        }

    }
    //后置通知
    @After("execution(* com.itheima.ssm.controller.*.*(..))")
    public void doAfter(JoinPoint jp) throws Exception {

        //获取访问时长
        long time = new Date().getTime() - visitTime.getTime();

        //获取url
        String url = "";
        if(clazz!=null && method!=null && clazz!= LogAop.class){
            //1 获取类上的@RequestMapping("/orders")
            RequestMapping classAnnotation = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
            if(classAnnotation!=null){
                String[] classValue = classAnnotation.value();
                //2 获取方法上的@RequestMapping(xxx)
                RequestMapping methodAnnotation = method.getAnnotation(RequestMapping.class);
                if(methodAnnotation!=null){
                    String[] methodValue = methodAnnotation.value();
                    url = classValue[0]+methodValue[0];  //拼接url /orders/findAll.do
                }
            }
        }
        //获取当前访问的ip地址
        String ip = request.getRemoteAddr();

        //获取当前操作的用户
        //SecurityContext context = request.getSession().getAttribute("SPRING_SECURITY_CONTEXT") 也可以通过request对象获取
        SecurityContext context = SecurityContextHolder.getContext(); //从上下文中获取当前登录的用户
        User user = (User) context.getAuthentication().getPrincipal();
        String username = user.getUsername();

        //将日志相关信息封装到SysLog对象
        SysLog syslog = new SysLog();
        syslog.setExecutionTime(time);
        syslog.setIp(ip);
        syslog.setMethod("[类名]"+clazz.getName()+"[方法名]"+method.getName());
        syslog.setUrl(url);
        syslog.setUsername(username);
        syslog.setVisitTime(visitTime);

        //调用service完成操作
        sysLogService.save(syslog);

        if(clazz.getName() == "com.itheima.ssm.controller.SysLogController"){
            return;
        }
        //调用service完成操作
        sysLogService.save(syslog);

    }
}
