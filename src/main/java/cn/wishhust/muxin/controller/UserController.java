package cn.wishhust.muxin.controller;

import cn.wishhust.muxin.pojo.Users;
import cn.wishhust.muxin.pojo.bo.UsersBO;
import cn.wishhust.muxin.pojo.vo.UsersVO;
import cn.wishhust.muxin.service.UserService;
import cn.wishhust.muxin.utils.FastDFSClient;
import cn.wishhust.muxin.utils.FileUtils;
import cn.wishhust.muxin.utils.IJSONResult;
import cn.wishhust.muxin.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/u")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    public IJSONResult registOrLogin(@RequestBody Users user) throws Exception {

        if(StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return IJSONResult.errorMsg("用户名和密码不能为空");
        }

        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist) {
            System.out.println(user.getUsername());
            System.out.println(user.getPassword());
            System.out.println(MD5Utils.getMD5Str(user.getPassword()));
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

    @PostMapping("/uploadFaceBase64")
    public IJSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {
        String base64Data = userBO.getFaceData();
        String userFacePath = "C:\\" + userBO.getUserId() + "userface64.png";
        FileUtils.base64ToFile(userFacePath,base64Data);

        // 上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

        // 获取缩略图url
        String thump = "_80x80.";
        String [] arr = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        //更新用户头像
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);

        Users result = userService.updateUserInfo(user);

        return IJSONResult.ok(result);
    }

    @PostMapping("/setNickname")
    public IJSONResult setNickname(@RequestBody UsersBO userBO) {

        // todo 对Nickname的判空
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());

        Users result = userService.updateUserInfo(user);

        return IJSONResult.ok(result);

    }

}
