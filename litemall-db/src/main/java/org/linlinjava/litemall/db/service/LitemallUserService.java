package org.linlinjava.litemall.db.service;

import com.github.pagehelper.PageHelper;
import org.linlinjava.litemall.db.dao.LitemallUserMapper;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.LitemallUserExample;
import org.linlinjava.litemall.db.domain.UserVo;
import org.linlinjava.litemall.db.util.CommonStatusConstant;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Service
public class LitemallUserService {
    @Resource
    private LitemallUserMapper userMapper;

    public LitemallUser findById(Integer userId) {
        return userMapper.selectByPrimaryKey(userId);
    }

    public String getUserFullName(Integer userId) {
        LitemallUser user = findById(userId);
        if (user == null) {
            return "";
        }
        StringBuilder fullName = new StringBuilder();
        if (!StringUtils.isEmpty(user.getUsername())) {
            fullName.append(user.getNickname());
        }
        if (!StringUtils.isEmpty(user.getUsername())) {
            fullName.append(" (").append(user.getUsername()).append(")");
        }
        return fullName.toString();
    }


    public List<LitemallUser> queryAllByDefaultTrader(LitemallTrader trader) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andDefaultTraderIdEqualTo(trader.getId()).andDeletedEqualTo(false);
        return userMapper.selectByExample(example);
    }

    public UserVo findUserVoById(Integer userId) {
        LitemallUser user = findById(userId);
        UserVo userVo = new UserVo();
        userVo.setNickname(user.getNickname());
        userVo.setAvatar(user.getAvatar());
        return userVo;
    }

    public LitemallUser queryByOid(String openId) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andWeixinOpenidEqualTo(openId).andDeletedEqualTo(false);
        return userMapper.selectOneByExample(example);
    }

    public Integer add(LitemallUser user) {
        user.setAddTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insertSelective(user);
        return user.getId();
    }

    public int updateById(LitemallUser user) {
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.updateByPrimaryKeySelective(user);
    }

    public List<LitemallUser> querySelective(String username, String mobile, Integer page, Integer size, String sort, String order) {
        LitemallUserExample example = new LitemallUserExample();
        LitemallUserExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(username)) {
            criteria.andUsernameLike("%" + username + "%");
        }
        if (!StringUtils.isEmpty(mobile)) {
            criteria.andMobileEqualTo(mobile);
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, size);
        return userMapper.selectByExample(example);
    }

    public int count() {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andDeletedEqualTo(false);

        return (int) userMapper.countByExample(example);
    }

    public List<LitemallUser> all() {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andDeletedEqualTo(false);
        return userMapper.selectByExample(example);
    }

    public List<LitemallUser> queryByIds(Integer[] userIds) {
        List<LitemallUser> users = new  LinkedList<LitemallUser>();
        if(userIds.length == 0){
            return users;
        }

        LitemallUserExample example = new LitemallUserExample();
        example.or().andIdIn(Arrays.asList(userIds)).andStatusEqualTo(CommonStatusConstant.STATUS_ENABLE).andDeletedEqualTo(false);
        users = userMapper.selectByExample(example);

        return users;
    }    

    /**
     * 查询除当前用户外的所有用户
     * @param userId
     * @return
     */
    public List<LitemallUser> queryOtherUsers(Integer userId) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andIdNotEqualTo(userId).andDeletedEqualTo(false);
        return userMapper.selectByExample(example);
    }    

    public List<LitemallUser> queryByUsername(String username) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andUsernameEqualTo(username).andDeletedEqualTo(false);
        return userMapper.selectByExample(example);
    }

    public boolean checkByUsername(String username) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andUsernameEqualTo(username).andDeletedEqualTo(false);
        return userMapper.countByExample(example) != 0;
    }

    public List<LitemallUser> queryByMobile(String mobile) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andMobileEqualTo(mobile).andDeletedEqualTo(false);
        return userMapper.selectByExample(example);
    }

    public List<LitemallUser> queryByOpenid(String openid) {
        LitemallUserExample example = new LitemallUserExample();
        example.or().andWeixinOpenidEqualTo(openid).andDeletedEqualTo(false);
        return userMapper.selectByExample(example);
    }

    public void deleteById(Integer id) {
        userMapper.logicalDeleteByPrimaryKey(id);
    }
}
