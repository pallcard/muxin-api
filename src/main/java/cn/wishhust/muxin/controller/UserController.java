package cn.wishhust.muxin.controller;

import cn.wishhust.muxin.enums.SearchFriendsStatusEnum;
import cn.wishhust.muxin.enums.OperatorFriendRequestTypeEnum;
import cn.wishhust.muxin.pojo.Users;
import cn.wishhust.muxin.pojo.bo.UsersBO;
import cn.wishhust.muxin.pojo.vo.MyFriendsVO;
import cn.wishhust.muxin.pojo.vo.UsersVO;
import cn.wishhust.muxin.service.UserService;
import cn.wishhust.muxin.utils.FastDFSClient;
import cn.wishhust.muxin.utils.FileUtils;
import cn.wishhust.muxin.utils.IJSONResult;
import cn.wishhust.muxin.utils.MD5Utils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/u")
@Api(value = "用户接口管理")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    @ApiOperation(value = "注册或登陆")
    public IJSONResult registOrLogin(@RequestBody Users user) throws Exception {

        if(StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getPassword())) {
            return IJSONResult.errorMsg("用户名和密码不能为空");
        }

        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if (usernameIsExist) {
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
    @ApiOperation("上传头像")
    public IJSONResult uploadFaceBase64(@RequestBody UsersBO userBO) throws Exception {
        String base64Data = userBO.getFaceData();
        String userFacePath = "F:\\" + userBO.getUserId() + "userface64.png";
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
    @ApiOperation("设置昵称")
    public IJSONResult setNickname(@RequestBody UsersBO userBO) {

        // todo 对Nickname的判空
        Users user = new Users();
        user.setId(userBO.getUserId());
        user.setNickname(userBO.getNickname());

        Users result = userService.updateUserInfo(user);

        return IJSONResult.ok(result);

    }

    @PostMapping("/search")
    @ApiOperation("搜索用户")
    public IJSONResult searchUser(String myUserId, String friendUsername) {

        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return IJSONResult.errorMsg("");
        }
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users user = userService.queryUserInfoByUsername(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(user, usersVO);
            return IJSONResult.ok(usersVO);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IJSONResult.errorMsg(errorMsg);
        }
    }

    @PostMapping("/addFriendRequest")
    @ApiOperation(value = "添加好友请求")
    public IJSONResult addFriendRequest(String myUserId, String friendUsername) {
        if (StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)) {
            return IJSONResult.errorMsg("");
        }
        Integer status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
           userService.sendFriendRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IJSONResult.errorMsg(errorMsg);
        }
        return IJSONResult.ok();
    }

    @PostMapping("/queryFriendRequests")
    @ApiOperation(value = "查询好友请求")
    public IJSONResult queryFriendRequests(String userId) {
        // 判空
        if (StringUtils.isBlank(userId)) {
            return IJSONResult.errorMsg("");
        }
        // 查询朋友申请
        return IJSONResult.ok(userService.queryFriendRequestList(userId));
    }

    @PostMapping("/operFriendRequest")
    @ApiOperation(value = "处理好友请求")
    public IJSONResult operFriendRequest(String acceptUserId, String sendUserId, Integer operType) {
        // 判空
        if (StringUtils.isBlank(acceptUserId)
                || StringUtils.isBlank(sendUserId)
                || null == operType) {
            return IJSONResult.errorMsg("");
        }
        // operType无对应枚举值
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IJSONResult.errorMsg("");
        }

        if (operType == OperatorFriendRequestTypeEnum.IGNORE.type) {
            // 忽略好友请求则删除数据库记录
            userService.deleteFriendRequest(sendUserId,acceptUserId);
        } else if(operType == OperatorFriendRequestTypeEnum.PASS.type) {
            userService.passFriendRequest(sendUserId, acceptUserId);
        }

        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);

        return IJSONResult.ok(myFriends);
    }

    @PostMapping("/myFriends")
    @ApiOperation(value = "查询我的朋友")
    public IJSONResult myFriends(String userId) {
        // 判空
        if (StringUtils.isBlank(userId)) {
            return IJSONResult.errorMsg("");
        }
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);

        return IJSONResult.ok(myFriends);
    }

}
