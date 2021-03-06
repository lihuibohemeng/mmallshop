package com.mmallshop.service.impl;

import com.mmallshop.common.Const;
import com.mmallshop.common.ServerResponse;
import com.mmallshop.common.TokenCache;
import com.mmallshop.dao.UserMapper;
import com.mmallshop.pojo.User;
import com.mmallshop.service.IUserService;
import com.mmallshop.util.MD5Util;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: 刘莉慧
 * Date: 2018/7/24
 * Time: 14:39
 * To change this template use File | Settings | File Templates.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUserName(username);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");

        }
//        密码登录 MD5
        User user = userMapper.selectLogin(username,password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }
//        password null
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);

    }


//    register
     @Override
    public ServerResponse<String> register(User user){
        ServerResponse serverResponse = this.checkVaild(user.getEmail(),Const.EMAIL);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        serverResponse = this.checkVaild(user.getUsername(),Const.USERNAME);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        user.setRole(Const.Role.ROLE_CUSTOMER);
        int resultCount = userMapper.insertSelective(user);
        if(resultCount >0 ){
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");

    }
    @Override
    public ServerResponse<String> checkVaild(String str,String type){
        if(StringUtils.isNotBlank(type)){
            if(Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("Email已存在");
                }
            }
                if(Const.USERNAME.equals(type)){
                  int   resultCount = userMapper.checkUserName(str);
                    if(resultCount > 0){
                        return ServerResponse.createByErrorMessage("用户名已存在");
                    }
            }

        }else {
            return  ServerResponse.createByErrorMessage("参数错误");
        }
        return  ServerResponse.createBySuccessMessage("校验成功");
    }
//    通过用户名找回用户设置的问题
    public ServerResponse<String> selectQuestion(String userName){
        ServerResponse vaildResponse = this.checkVaild(userName,Const.USERNAME);
        if(vaildResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        String question = userMapper.selectQuestion(userName);
        if(StringUtils.isNotBlank(question)){
            return  ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回问题为空");
    }

//    用户输入问题的密码，返回Token值
    public ServerResponse<String> checkAnswer(String userName, String answer,String question){
         int resultCount = userMapper.checkAnswer(userName,answer,question);
         if(resultCount>0){
             String forgetToken = UUID.randomUUID().toString();
             TokenCache.setKey(TokenCache.TOKEN_PREFIX+userName,forgetToken);
             return ServerResponse.createBySuccess(forgetToken);
         }
        return ServerResponse.createByErrorMessage("答案错误");

    }

//    通过Token+userName+password 来更新密码
//    第一步  我觉得是先验证token 是不是正确的
//    第二步  验证用户 然后更新密码

    public ServerResponse<String> forgetResetPassword(String userName,String password,String forgetToken){
        if(!StringUtils.isNotBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("Token不能为空");
        }
        ServerResponse validResponse = this.checkVaild(userName,Const.USERNAME);
        if(validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+forgetToken);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("Token无效");
        }
        if(StringUtils.equals(forgetToken,token)){
            String md5Password  = MD5Util.MD5EncodeUtf8(password);
            int resultCount = userMapper.updatePasswordByUsername(userName,password);
            if(resultCount>0){
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
        }else{
            return ServerResponse.createByErrorMessage("token不一致");
        }
        return ServerResponse.createByErrorMessage("失败");

    }

}
