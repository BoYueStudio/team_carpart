package com.xiupeilian.carpart.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.Callback;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.sun.tools.internal.ws.wsdl.document.jaxws.Exception;
import com.xiupeilian.carpart.constant.SysConstant;
import com.xiupeilian.carpart.model.*;
import com.xiupeilian.carpart.service.BrandService;
import com.xiupeilian.carpart.service.CityService;
import com.xiupeilian.carpart.service.UserService;
import com.xiupeilian.carpart.task.MailTask;
import com.xiupeilian.carpart.util.SHA1Util;
import com.xiupeilian.carpart.vo.LoginVo;
import com.xiupeilian.carpart.vo.RegisterVo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.web.multipart.MultipartFile;
import com.aliyun.oss.model.Callback.CalbackBodyType;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSenderImpl mailSender;
    @Autowired
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private CityService cityService;
    @Autowired
    private BrandService brandService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 前往登录页面
     * @return
     */
    @RequestMapping("/toLogin")
    public String toLogin(){

        return "login/login";
    }

    @RequestMapping("/login")
    public void Login(LoginVo vo, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code=(String)request.getSession().getAttribute(SysConstant.VALIDATE_CODE);
        if(code.toUpperCase().equals(vo.getValidate().toUpperCase())){//验证 验证码

            //基于url拦截登录
//            vo.setPassword(SHA1Util.encode(vo.getPassword()));//SHA1加密，保证数据库里的信息不是明文
//            SysUser user=userService.findUserByVo(vo);
//            if(user!=null){
//                request.getSession().setAttribute("user",user);
//                response.getWriter().write("3");
//            }else{//用户或密码错误
//                response.getWriter().write("2");
//            }

            //基于shiro权限认证授权登录
            Subject subject=SecurityUtils.getSubject();
            UsernamePasswordToken toke=new UsernamePasswordToken(vo.getLoginName(),vo.getPassword());

            try {
                subject.login(toke);
            } catch (AuthenticationException e) {
                //用户密码错误
                response.getWriter().write(e.getMessage());
                return;
            }
            //获取存入的用户信息
            SysUser user=(SysUser)SecurityUtils.getSubject().getPrincipal();
            request.getSession().setAttribute("user",user);//存到spring-session中
            response.getWriter().write("3");


        }else{
            //验证码错误
            response.getWriter().write("1");
        }

    }

    @RequestMapping("/noauth")
    public String noauth(){
        return "exception/noauth";
    }

    @RequestMapping("/forgetPassword")
    public String forgetPassword(){

        return "login/forgetPassword";
    }

    @RequestMapping("/getPassword")
    public void getPassword(HttpServletRequest request,HttpServletResponse response,LoginVo vo) throws IOException, MessagingException {

        SysUser user=userService.findUserByUsernameAndEmail(vo);
        if(user==null){
            //账号和邮箱不匹配
            response.getWriter().write("1");
        }else{
            //同步修改密码流程
            //1.设置新密码
            String password=new Random().nextInt(899999)+1000000+"";


            //2.修改数据库
            user.setPassword(SHA1Util.encode(password));
            userService.updateUser(user);

            //3.发送邮箱
//            SimpleMailMessage message=new SimpleMailMessage();
//            message.setFrom("lover326@sina.cn");
//            message.setTo(user.getEmail());
//            message.setSubject("博悦网络工作室登录密码修改");
//            message.setText("您的新密码为："+password);
//            //mailSender.setDefaultEncoding("utf-8");
//            mailSender.send(message);
//
//            MailTask mailTask=new MailTask(mailSender,message);
//
//            executor.execute(mailTask);



            MimeMessage message=mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("lover326@sina.cn");
            helper.setTo(user.getEmail());
            helper.setSubject("博悦网络工作室登录密码修改");
            helper.setText("您的新密码为："+password);
            mailSender.send(message);



            response.getWriter().write("2");
        }

    }

    @RequestMapping("/toRegister")
    public String toRegister(HttpServletRequest request){
        //初始化数据  汽车品牌、配件种类、精品种类
        List<Brand> brandList=brandService.findBrandAll();
        List<Parts> partsList=brandService.findPartsAll();
        List<Prime> primeList=brandService.findPrimeAll();
        request.setAttribute("brandList",brandList);
        request.setAttribute("partsList",partsList);
        request.setAttribute("primeList",primeList);

        return "login/register";
    }

    @RequestMapping("/checkLoginName")
    public void checkLoginName(String loginName,HttpServletResponse response) throws IOException {
        //根据账号去数据库查询，看此账号是否存在
        SysUser user=userService.findUserByLoginName(loginName);
        if(null==user){
            response.getWriter().write("1");
        }else{
            response.getWriter().write("2");
        }
    }

    @RequestMapping("/checkPhone")
    public void checkPhone(String telnum,HttpServletResponse response) throws IOException {
        //根据手机号去数据库查询
        SysUser user=userService.findUserByPhone(telnum);
        if(null==user){
            response.getWriter().write("1");
        }else{
            response.getWriter().write("2");
        }
    }


    @RequestMapping("/checkEmail")
    public void checkEmail(String email,HttpServletResponse response) throws IOException {
        //根据手机号去数据库查询
        SysUser user=userService.findUserByEmail(email);
        if(null==user){
            response.getWriter().write("1");
        }else{
            response.getWriter().write("2");
        }
    }

    @RequestMapping("/checkCompanyname")
    public void checkCompanyname(String companyname,HttpServletResponse response) throws IOException {
        //校验企业名称是否注册过
        Company company=userService.findCompanyByName(companyname);
        if(null==company){
            response.getWriter().write("1");
        }else{
            response.getWriter().write("2");
        }
    }

    /**
     * @Description: 省市县三级联动，根据父id查询所有的子节点
     * @Author:      Administrator
     * @Param:
     * @Return
     **/
    @RequestMapping("/getCity")
    public @ResponseBody
    List<City> getCity(Integer parentId){
        parentId=parentId==null?SysConstant.CITY_CHINA_ID:parentId;
        List<City> cityList=cityService.findCitiesByParentId(parentId);
        return cityList;
    }

    @RequestMapping("/register")
    public String register(RegisterVo vo){
        //参数
        //先插入企业表，再插入用户表,需要在一个事务进行控制
        userService.addRegsiter(vo);
        return "redirect:toLogin";
    }

    @RequestMapping("/toPhoneLogin")
    public String toPhoneLogin(){
        return "login/phoneLogin";
    }

    @RequestMapping("/sendMessage")
    public  void sendMessage(String phone,HttpServletResponse resp,HttpServletRequest req) throws IOException {

        SysUser user=userService.findUserByPhone(phone);
        if(user!=null) {

            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", "LTAI8J3o3KISsBPC", "ENNyadCg7HJMObqQYHiJHozY6I7lhq");
            IAcsClient client = new DefaultAcsClient(profile);

            String code = new Random().nextInt(899999) + 100000 + "";
            CommonRequest request = new CommonRequest();
            request.setMethod(MethodType.POST);
            request.setDomain("dysmsapi.aliyuncs.com");
            request.setVersion("2017-05-25");
            request.setAction("SendSms");
            request.putQueryParameter("RegionId", "cn-hangzhou");
            request.putQueryParameter("PhoneNumbers", phone);
            request.putQueryParameter("SignName", "\u7d2b\u8587\u82b1\u5f00\u4e86");
            request.putQueryParameter("TemplateCode", "SMS_172888946");
            request.putQueryParameter("TemplateParam", "{\"code\":\"" + code + "\"}");

            try {
                CommonResponse response = client.getCommonResponse(request);
                System.out.println(response.getData());
                redisTemplate.boundValueOps("Phone:" + phone).set(code);
                redisTemplate.expire("Phone:" + phone, 1, TimeUnit.MINUTES);//验证码失效时间
                req.getSession().setAttribute("user",user);//存储登录user

                resp.getWriter().write("1");//已发送短信
            } catch (ServerException e) {
                e.printStackTrace();
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }else{
            resp.getWriter().write("2");//手机号没有

        }
    }

    @RequestMapping("/messageQuery")
    public void messageQuery(HttpServletRequest request, HttpServletResponse response,String phone,String code) throws IOException {
        Object telAuth=redisTemplate.boundValueOps("Phone:"+phone).get();
        if(telAuth==""||telAuth==null){
            response.getWriter().write("1");
        }else if(telAuth.equals(code)){

            SysUser vo=userService.findUserByPhone(phone);
            vo.setPassword("123123");
            Subject subject=SecurityUtils.getSubject();
            UsernamePasswordToken token=new UsernamePasswordToken(vo.getLoginName(),vo.getPassword());
            try {
                subject.login(token);
            } catch (AuthenticationException e) {
                //用户密码错误
                response.getWriter().write("3");
                return;
            }
            SysUser user=(SysUser)SecurityUtils.getSubject().getPrincipal();
            request.getSession().setAttribute("user",user);//存到spring-session中
            response.getWriter().write("2");
        }else{
            response.getWriter().write("3");
        }

    }
    //分布式缓存管理test
    @RequestMapping("/test")
    public void test(int id){
        cityService.deleteCityById(id);
    }

    //前往oss上传页面
    @RequestMapping("/toOssText")
    public String toOssTest(){
        return "login/ossTest";
    }


    //oss 上传测试
    @RequestMapping("/ossTest")
    public void ossTest(MultipartFile file,HttpServletResponse response) throws IOException {

        String fileName=file.getOriginalFilename();//获取文件名称
        String suffixName=fileName.substring(fileName.lastIndexOf("."));//取上传文件的后缀
        String newName=UUID.randomUUID().toString()+suffixName;//随机生成文件名

        String endpoint = "http://oss-cn-beijing.aliyuncs.com";
// 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
        String accessKeyId = "LTAI8J3o3KISsBPC";
        String accessKeySecret = "ENNyadCg7HJMObqQYHiJHozY6I7lhq";
        String bucketName = "2019boyue";//筒的名称
        String objectName = newName+suffixName;//上传的文件
        String url="";
        try{
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        // 上传内容到指定的存储空间（bucketName）并保存为指定的文件名称（objectName）。
        ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(file.getBytes()));
        // 关闭OSSClient。
        ossClient.shutdown();
        //设置URL过期时间为10年  3600l* 1000*24*365*10
        Date expiration = new Date(new Date().getTime() + 3600l * 1000 * 24 * 365 * 10);
        // 生成URL
        url=ossClient.generatePresignedUrl(bucketName, objectName, expiration).toString();
        } catch (IOException  e) {
            e.printStackTrace();
            //上传失败
        }

        response.getWriter().write("上传成功！"+url);

    }

    @RequestMapping("/callBack")
    public void callBack(){

    }



}
