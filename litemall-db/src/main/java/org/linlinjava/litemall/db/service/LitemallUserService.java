package org.linlinjava.litemall.db.service;

import com.github.pagehelper.PageHelper;
import org.linlinjava.litemall.db.dao.LitemallUserMapper;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.LitemallUserExample;
import org.linlinjava.litemall.db.domain.UserVo;
import org.linlinjava.litemall.db.util.CommonStatusConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
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

    public String encryptUserNameAndPhone(String directorName) {
        LitemallUser user = new LitemallUser();
        directorName = directorName == null ? "" : directorName;
        String  str = directorName.replace("（", "(").replace("）", ")");
        String[] list = str.split("[()]");
        if (list.length == 2) {
            user.setNickname(list[0]);
            String phone = list[1];
            user.setMobile(phone);
            return encryptUserNameAndPhone(user);
        } else {
            user.setNickname("");
            user.setMobile(directorName);
        }
        return encryptUserNameAndPhone(user);
    }

    public String encryptUserNameAndPhone(LitemallUser user) {
        String userNickname = user.getNickname();
        String phone = user.getMobile();

        StringBuilder sb = new StringBuilder();
        //如果用户名是两个字，则第二字母改为*，如果是三个字，则第二个字母改为*，如果是四个字以上，则第二和第三个字母改为**
        String userNameEncrypt = userNickname;
        if (userNickname.length() == 2) {
            userNameEncrypt = userNickname.substring(0, 1) + "*" + userNickname.substring(2);
        } else if (userNickname.length() == 3) {
            userNameEncrypt = userNickname.substring(0, 1) + "*" + userNickname.substring(2);
        } else if (userNickname.length() >= 4) {
            userNameEncrypt = userNickname.substring(0, 1) + "**" + userNickname.substring(3);
        }
        //手机号前三位，后四位，中间四位改为****
        sb.append(userNameEncrypt).append(" (").append(encryptTel(phone)).append(")");

        return sb.toString();
    }

    public String encryptTel(String tel) {
        //手机号前三位，后四位，中间四位改为****
        if (tel == null) {
            return tel;
        }
        int len = tel.length();

        // 11位手机号：保留前三位和后四位，中间改为 ****
        if (len == 11) {
            return tel.substring(0, 3) + "****" + tel.substring(7);
        }

        // 小于7位的座机号：全部返回
        if (len < 7) {
            return tel;
        }

        // 长度为7或8：保留前1位和后3位，中间用*号代替
        if (len <= 8) {
            int middleStars = len - 4; // 中间星号数
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < middleStars; i++) {
            sb.append('*');
            }
            return tel.substring(0, 1) + sb.toString() + tel.substring(len - 3);
        }

        // 大于8位：保留前3位和后4位，中间全部用*号
        int middleStars = len - 7; // 中间星号数
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < middleStars; i++) {
            sb.append('*');
        }
        return tel.substring(0, 3) + sb.toString() + tel.substring(len - 4);
    }
}
