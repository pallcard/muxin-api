package cn.wishhust.muxin.service.impl;

import cn.wishhust.muxin.mapper.UsersMapper;
import cn.wishhust.muxin.pojo.Users;
import cn.wishhust.muxin.service.UserService;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private Sid sid;

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
        usersMapper.selectByExample(userExample);

        return null;
    }

    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();
        // TODO 为每一个用户生成一个唯一的二维码
        user.setQrcode("");
        user.setId(userId);
        usersMapper.insert(user);

        return user;
    }
}
