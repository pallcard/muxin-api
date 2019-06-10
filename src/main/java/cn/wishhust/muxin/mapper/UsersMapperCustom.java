package cn.wishhust.muxin.mapper;

import cn.wishhust.muxin.pojo.Users;
import cn.wishhust.muxin.pojo.vo.FriendRequestVO;
import cn.wishhust.muxin.pojo.vo.MyFriendsVO;
import cn.wishhust.muxin.utils.MyMapper;

import java.util.List;

// 自定义sql
public interface UsersMapperCustom extends MyMapper<Users> {

	public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

	public List<MyFriendsVO> queryMyFriends(String userId);

	public void batchUpdateMsgSigned(List<String> msgIdList);

}
