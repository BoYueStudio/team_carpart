package com.xiupeilian.carpart.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xiupeilian.carpart.model.Dymsn;
import com.xiupeilian.carpart.model.Menu;
import com.xiupeilian.carpart.model.Notice;
import com.xiupeilian.carpart.model.SysUser;
import com.xiupeilian.carpart.service.DymsnService;
import com.xiupeilian.carpart.service.MenuService;
import com.xiupeilian.carpart.service.UserService;
import com.xiupeilian.carpart.task.MailTask;
import com.xiupeilian.carpart.util.SHA1Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/index")
public class IndexController {
    @Autowired
    private UserService userService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private DymsnService dymsnService;
    @Autowired
    private JavaMailSenderImpl mailSender;
    @Autowired
    private ThreadPoolTaskExecutor executor;
    /**
     * 登录后前往index
     * @return
     */
    @RequestMapping("toIndex")
    public String toIndex(){

        return "index/index";
    }

    /**
     * 首页top
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("top")
    public String top(HttpServletRequest request,HttpServletResponse response){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        String now=sdf.format(new Date());

        request.setAttribute("now",now);
        return "index/top";
    }

    /**
     * 导航信息
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("navigation")
    public String navigation(HttpServletRequest request,HttpServletResponse response){
        SysUser user=(SysUser)request.getSession().getAttribute("user");
        Integer id=user.getId();
        List<Menu> menuList=menuService.findMenu(id);
        request.setAttribute("menuList",menuList);

        return "index/navigation";
    }

    /**
     * 动态消息list
     * @param response
     * @param request
     * @return
     */
    @RequestMapping("dymsn")
    public String dymsn(HttpServletResponse response,HttpServletRequest request){
        List<Dymsn> list= dymsnService.findDymsn();
        request.setAttribute("list",list);
        return "index/dymsn";
    }

    @RequestMapping("notice")
    public String notice(Integer pageSize,Integer pageNum, HttpServletRequest request){
        pageSize=pageSize==null?10:pageSize;
        pageNum=pageNum==null?1:pageNum;
        PageHelper.startPage(pageNum,pageSize);

        List<Notice> list=dymsnService.findNotice();
        PageInfo<Notice> page=new PageInfo<>(list);
        request.setAttribute("page",page);
        return "index/notice";
    }
    @RequestMapping("/toChangePassword")
    public String toChangePassword() {

        return "index/changePassword";

    }

    @RequestMapping("/checkPassword")
    public void checkPassword(HttpServletRequest request, HttpServletResponse response, String oldPwd) throws IOException {

        System.out.println(oldPwd);

        String loginName = (String) request.getSession().getAttribute("loginName");

        System.out.println(loginName);

        SysUser user = userService.findUserByLoginName(loginName);

        if (user.getPassword().equals(SHA1Util.encode(new String(oldPwd)))) {

            //生成新密码
            String password = new Random().nextInt(899999) + 100000 + "";
            //修改数据库
            user.setPassword(SHA1Util.encode(password));
            userService.updateUser(user);
            //发送邮件到数据库
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("wang_shuiyi@sina.com");
            message.setTo(user.getEmail());
            message.setSubject("修配连汽配市场密码找回功能:");
            message.setText("您的新密码是:" + password);
            // mailSender.send(message);
            MailTask mailTask = new MailTask(mailSender, message);
            //让线程池去执行该任务
            executor.execute(mailTask);
            response.getWriter().write("2");

        } else {

            response.getWriter().write("1");

        }

    }

    @RequestMapping("/changePassword")
    public void changePassword(HttpServletRequest request, HttpServletResponse response, String newPwd) throws IOException {
        String loginName = (String) request.getSession().getAttribute("loginName");
        SysUser user = new SysUser();
        user.setLoginName(loginName);
        user.setPassword(SHA1Util.encode(new String(newPwd)));
        userService.updatePasswordByLoginName(user);
        response.getWriter().write("1");

    }

}
