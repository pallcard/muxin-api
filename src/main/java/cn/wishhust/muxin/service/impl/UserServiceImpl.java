package cn.wishhust.muxin.service.impl;

import cn.wishhust.muxin.enums.MsgActionEnum;
import cn.wishhust.muxin.enums.SearchFriendsStatusEnum;
import cn.wishhust.muxin.enums.MsgSignFlagEnum;
import cn.wishhust.muxin.mapper.*;
import cn.wishhust.muxin.netty.ChatMsg;
import cn.wishhust.muxin.netty.DataContent;
import cn.wishhust.muxin.netty.UserChannelRel;
import cn.wishhust.muxin.pojo.FriendsRequest;
import cn.wishhust.muxin.pojo.MyFriends;
import cn.wishhust.muxin.pojo.Users;
import cn.wishhust.muxin.pojo.vo.FriendRequestVO;
import cn.wishhust.muxin.pojo.vo.MyFriendsVO;
import cn.wishhust.muxin.service.UserService;
import cn.wishhust.muxin.utils.FastDFSClient;
import cn.wishhust.muxin.utils.FileUtils;
import cn.wishhust.muxin.utils.JsonUtils;
import cn.wishhust.muxin.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {

        Users user = new Users();
        user.setUsername(username);
        Users result = usersMapper.selectOne(user);

        return result != null ? true : false;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String pwd) {
        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username", username);
        criteria.andEqualTo("password", pwd);
        Users result = usersMapper.selectOneByExample(userExample);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();
        // 为每一个用户生成一个唯一的二维码
        // muxin_qrcode:[username]
        String qrCodePath = "F://user"+userId+"qrcode.png";
        qrCodeUtils.createQRCode(qrCodePath,"muxin_qrcode:"+user.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);

        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Users queryUserById(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUsername) {

        Users user = queryUserInfoByUsername(friendUsername);

        // 1.搜索用户不存在
        if (null == user) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        // 2.搜索用户为自己
        if (myUserId.equals(user.getId())) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        // 3.搜索用户是自己的好友
        Example mfe = new Example(MyFriends.class);
        Example.Criteria mfc = mfe.createCriteria();
        mfc.andEqualTo("myUserId", myUserId);
        mfc.andEqualTo("myFriendUserId", user.getId());
        MyFriends myFriendsRel = myFriendsMapper.selectOneByExample(mfe);
        if (null != myFriendsRel) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }

        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username) {
        Example ue = new Example(Users.class);
        Example.Criteria uc = ue.createCriteria();
        uc.andEqualTo("username", username);
        return usersMapper.selectOneByExample(ue);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {

        Users friend = queryUserInfoByUsername(friendUsername);

        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId",myUserId);
        frc.andEqualTo("acceptUserId",friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);
        if (null == friendsRequest) {
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria frc = fre.createCriteria();
        frc.andEqualTo("sendUserId", sendUserId);
        frc.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(sendUserId, acceptUserId);
        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (null != sendChannel) {
            // 使用websocket主动推送消息到请求发起者，更新他的通讯录列表
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyUserId(sendUserId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriendsMapper.insert(myFriends);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriends = usersMapperCustom.queryMyFriends(userId);
        return myFriends;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {

        cn.wishhust.muxin.pojo.ChatMsg msgDB = new cn.wishhust.muxin.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());
        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
       usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Override
    public List<cn.wishhust.muxin.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {
        Example chatExample = new Example(cn.wishhust.muxin.pojo.ChatMsg.class);
        final Example.Criteria chatCriteria = chatExample.createCriteria();
        chatCriteria.andEqualTo("signFlag", 0);
        chatCriteria.andEqualTo("acceptUserId",acceptUserId);
        List<cn.wishhust.muxin.pojo.ChatMsg> result = chatMsgMapper.selectByExample(chatExample);
        return result;
    }

}
