package cn.wishhust.muxin.service;

import cn.wishhust.muxin.pojo.Users;

public interface UserService {
    // 判断用户名是否存在
    public boolean queryUsernameIsExist(String username);

    // 查询用户是否存在
    public Users queryUserForLogin(String username, String pwd);

    // 用户注册
    public Users saveUser(Users user);

    // 修改用户记录
    public Users updateUserInfo(Users user);

    // 搜索朋友的前置条件
    public Integer preconditionSearchFriends(String myUserId, String friendUsername);

    // 根据用户名查用户对象
    public Users queryUserInfoByUsername(String username);

    // 添加好友请求记录，保存到数据库
    public void sendFriendRequest(String myUserId, String friendUsername);
}
