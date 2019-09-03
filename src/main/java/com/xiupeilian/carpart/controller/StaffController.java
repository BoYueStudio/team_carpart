package com.xiupeilian.carpart.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xiupeilian.carpart.model.*;
import com.xiupeilian.carpart.service.UserService;
import com.xiupeilian.carpart.vo.RegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @Description: 员工管理模板
 * @Author: Tu Xu
 * @CreateDate: 2019/8/22 9:44
 * @Version: 1.0
 **/
@Controller
@RequestMapping("/staff")
public class StaffController {
    @Autowired
    private UserService userService;

    @RequestMapping("/staffList")
    public String staffList(String username,Integer pageSize, Integer pageNum, HttpServletRequest request) {
        SysUser user = new SysUser();
        user.setUsername(username);
        pageSize=pageSize==null?10:pageSize;
        pageNum=pageNum==null?1:pageNum;
        PageHelper.startPage(pageNum,pageSize);
        //查询全部
        List<SysUser> list=userService.findStaff(user);
        PageInfo<SysUser> page=new PageInfo<>(list);
        request.setAttribute("page",page);
        request.setAttribute("username",username);
        return "staff/staffList";
    }
    @RequestMapping("/addStaff")
    public String addStaff(RegisterVo vo){
        userService.addRegsiter(vo);
        return "redirect:staffList";
    }
    @RequestMapping("/toAddStaff")
    public String toAddStaff(HttpServletRequest request){

        return "staff/addStaff";
    }

    @RequestMapping("/toEditStaff")
    public String toEditStaff(@RequestParam("id") int id, HttpServletRequest request){

        SysUser user = userService.findStaffById(id);

        Company company = userService.findCompanyById(user.getCompanyId());

        request.setAttribute("user",user);

        request.setAttribute("salebusiness",company);

        return "staff/editStaff";

    }

    @RequestMapping("/editStaff")
    public String editStaff(SysUser staff) {

        System.out.println("---------");
        System.out.println(staff);

        userService.updateUserById(staff);

        return "redirect:staffList";
    }
}
