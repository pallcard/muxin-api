package cn.wishhust.muxin.controller;

import cn.wishhust.muxin.pojo.Users;
import cn.wishhust.muxin.pojo.vo.UsersVO;
import cn.wishhust.muxin.service.UserService;
import cn.wishhust.muxin.utils.IJSONResult;
import cn.wishhust.muxin.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/u")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/registOrLogin")
    public IJSONResult registOrLogin(@RequestBody Users user) throws Exception {

        if(StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return IJSONResult.errorMsg("用户名和密码不能为空");
        }

        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist){
            // 登录
            userResult = userService.queryUserForLogin(user.getUsername(),
                    MD5Utils.getMD5Str(user.getPassword()));
            if (null == userResult) {
                return IJSONResult.errorMsg("用户名或密码不正确。。。");
            }
        } else {
            // 注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(userResult, usersVO);
        return IJSONResult.ok(usersVO);
    }
}
