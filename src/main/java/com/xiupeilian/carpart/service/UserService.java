package com.xiupeilian.carpart.service;

import com.xiupeilian.carpart.model.Company;
import com.xiupeilian.carpart.model.Role;
import com.xiupeilian.carpart.model.SysUser;
import com.xiupeilian.carpart.vo.LoginVo;
import com.xiupeilian.carpart.vo.RegisterVo;

import java.util.List;

public interface UserService {

    SysUser findUserByVo(LoginVo vo);

    SysUser findUserByUsernameAndEmail(LoginVo vo);

    void updateUser(SysUser user);

    SysUser findUserByLoginName(String loginName);

    SysUser findUserByPhone(String telnum);

    SysUser findUserByEmail(String email);

    Company findCompanyByName(String companyname);

    void addRegsiter(RegisterVo vo);

    Role findRoleByRoleId(Integer roleId);

    SysUser findStaffById(Integer id);

    Company findCompanyById(Integer companyId);

    void updateUserById(SysUser staff);

    void updatePasswordByLoginName(SysUser user);

    List<SysUser> findStaff(SysUser user);
}
