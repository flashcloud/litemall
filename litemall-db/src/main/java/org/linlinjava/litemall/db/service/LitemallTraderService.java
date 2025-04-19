package org.linlinjava.litemall.db.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.linlinjava.litemall.db.dao.LitemallTraderMapper;
import org.linlinjava.litemall.db.domain.LitemallTraderExample;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.util.CommonStatusConstant;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import com.github.pagehelper.PageHelper;

@Service
public class LitemallTraderService {
    @Autowired
    private LitemallUserService userService;

    @Autowired
    private LitemallOrderService orderService;

    @Resource
    private LitemallTraderMapper traderMapper;
    public List<LitemallTrader> querySelective(String name, Integer page, Integer limit, String sort, String order) {
        LitemallTraderExample example = new LitemallTraderExample();
        LitemallTraderExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(name)) {
            criteria.andCompanyNameLike("%" + name + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return traderMapper.selectByExample(example);
    }

    public LitemallTrader queryById(Integer id) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andIdEqualTo(id).andDeletedEqualTo(false);
        return traderMapper.selectOneByExample(example);
    }

    public List<LitemallTrader> queryByIds(Integer[] traderIds) {
        List<LitemallTrader> traders = new  LinkedList<LitemallTrader>();
        if(traderIds.length == 0){
            return traders;
        }

        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andIdIn(Arrays.asList(traderIds)).andStatusEqualTo(CommonStatusConstant.STATUS_ENABLE).andDeletedEqualTo(false);
        traders = traderMapper.selectByExample(example);

        return traders;
    }

    public void add(LitemallTrader trader) {
        trader.setAddTime(LocalDateTime.now());
        trader.setUpdateTime(LocalDateTime.now());
        traderMapper.insertSelective(trader);
        
        // 如果是新增商户，在商户负责人的商户列表中添加该商户
        if (trader.getUserId() != null) {
            LitemallUser user = userService.findById(trader.getUserId());
            if (user != null) {
                Integer[] traderIds = user.getTraderIds();
                traderIds = Arrays.copyOf(traderIds, traderIds.length + 1);
                traderIds[traderIds.length - 1] = trader.getId();
                user.setTraderIds(traderIds);
                userService.updateById(user);
            }
        }
    }

    public int updateById(LitemallTrader trader) {
        trader.setUpdateTime(LocalDateTime.now());
        return traderMapper.updateByPrimaryKeySelective(trader);
    }

    public void deleteById(Integer id) {
        // 如果该商户有用户在使用，不能删除
        if (isTraderUsed(id)) {
            return;
        }

        deleteHlp(id);
    }

    /**
     * 如果该商户有用户在使用，不能删除
     * @param loginUserId
     * @param id
     */
    public void deleteByUser(Integer loginUserId, Integer id) {
        if (isTraderUsedByOtherUsers(loginUserId, id)) {
            return;
        }

        //移除用户的商户
        LitemallUser user = userService.findById(loginUserId);
        if (user != null) {
            Integer[] traderIds = user.getTraderIds();
            if (traderIds != null) {
                List<Integer> traderIdList = new ArrayList<>(Arrays.asList(traderIds));
                traderIdList.remove(id);
                traderIds = traderIdList.toArray(new Integer[0]);
                user.setTraderIds(traderIds);
                userService.updateById(user);
            }
            //TODO: 设置默认商户
        }

        deleteHlp(id);
    }

    private void deleteHlp(Integer id) {
        //如果当前商户有订单，不能删除
        if (orderService.countByTrader(id) > 0)  return;

         //将该商户的的税号后面更新为随机字符串
        LitemallTrader trader = queryById(id);
        if (trader == null) {
            return;
        }
        trader.setTaxid(trader.getTaxid() + "--@@--" + RandomStringUtils.randomAlphanumeric(20));
        updateById(trader);

        traderMapper.logicalDeleteByPrimaryKey(id);
    }

    /**
     * 检查商户是否被使用
     * @param id
     * @return
     */
    public boolean isTraderUsed(Integer id) {
        return isTraderUsedHlp(0, id);
    }

    public boolean isTraderUsedByOtherUsers(Integer userId, Integer id) {
        return isTraderUsedHlp(userId, id);
    }
    
    /**
     * 使用了指定商户的所有用户
     * @param id
     * @return
     */
    public List<LitemallUser> usedTraderByUsers(Integer id) {
        return usedTraderByUsersHlp(0, id);
    }
    public Integer[] usedTraderByUserIds(Integer id) {
        List<LitemallUser>  userList = usedTraderByUsers(id);
        Integer[] userIds = new Integer[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            userIds[i] = userList.get(i).getId();
        }
        return userIds;
    }

    /**
     * 使用了指定商户的其他用户
     * @param userId
     * @param id
     * @return
     */
    public List<LitemallUser> usedTraderByOtherUsers(Integer userId, Integer id) {
        return usedTraderByUsersHlp(userId, id);
    }

    public String usedTraderUsersString(List<LitemallUser> userList) {
        StringBuilder sb = new StringBuilder();
        if (userList != null && userList.size() > 0) {
            for (LitemallUser user : userList) {
                //如果是最后一个用户，不要加逗号
                sb.append(user.getUsername()).append('(').append(user.getNickname()).append(')');
                if (userList.indexOf(user) != userList.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        return sb.toString();
    }
    
    private List<LitemallUser> usedTraderByUsersHlp(Integer userId, Integer id) {
        Set<LitemallUser> usedUsers = new HashSet<LitemallUser>();

        List<LitemallUser> users = null;
        if (userId != null && userId > 0) {
            users = userService.queryOtherUsers(userId);
        } else {
            users = userService.all();
        }
        if (users == null) {
            return new ArrayList<>(usedUsers);
        }
        for (LitemallUser user : users) {
            Integer[] traderIds = user.getTraderIds();
            if (traderIds != null) {
                for (Integer traderId : traderIds) {
                    if (traderId.equals(id)) {
                        usedUsers.add(user);
                    }
                }
            }
        }
        return new ArrayList<>(usedUsers);
    }

    private boolean isTraderUsedHlp(Integer userId, Integer id) {
        List<LitemallUser> users = null;
        if (userId != null && userId > 0) {
            users = userService.queryOtherUsers(userId);
        } else {
            users = userService.all();
        }
        if (users == null) {
            return false;
        }
        for (LitemallUser user : users) {
            Integer[] traderIds = user.getTraderIds();
            if (traderIds != null) {
                for (Integer traderId : traderIds) {
                    if (traderId.equals(id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<LitemallTrader> queryAll() {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andDeletedEqualTo(false);
        return traderMapper.selectByExample(example);
    }

    public boolean checkCompanyExist(String name) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andCompanyNameEqualTo(name).andDeletedEqualTo(false);
        return traderMapper.countByExample(example) != 0;
    }

    public boolean checkCompanyExist(String name, Integer id) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andIdNotEqualTo(id).andCompanyNameEqualTo(name).andDeletedEqualTo(false);
        return traderMapper.countByExample(example) != 0;
    }

    /**
     * 检查税号是否存在
     * @param taxid
     * @return
     */
    public boolean checkTaxidExist(String taxid) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andTaxidEqualTo(taxid).andDeletedEqualTo(false);
        return traderMapper.countByExample(example) != 0;
    }

    /**
     * 检查税号是否存在
     * @param taxid
     * @return
     */
    public boolean checkTaxidExist(String taxid, Integer id) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andIdNotEqualTo(id).andTaxidEqualTo(taxid).andDeletedEqualTo(false);
        return traderMapper.countByExample(example) != 0;
    }

    public boolean validate(LitemallTrader trader) {
        if (trader.getCompanyName() != null) trader.setCompanyName(trader.getCompanyName().trim().replace(" ", ""));
        if (trader.getNickname() != null) trader.setNickname(trader.getNickname().trim().replace(" ", ""));
        if (trader.getTaxid() != null) trader.setTaxid(trader.getTaxid().trim().replace(" ", ""));
        if (trader.getPhoneNum() != null) trader.setPhoneNum(trader.getPhoneNum().trim().replace(" ", ""));
        if (trader.getAddress() != null) trader.setAddress(trader.getAddress().trim());  //地址不需要去掉空格

		String companyName = trader.getCompanyName();
		if (StringUtils.isEmpty(companyName)) {
			return false;
		}
		String nickname = trader.getNickname();
		if (StringUtils.isEmpty(nickname)) {
			return false;
		}
        String taxid = trader.getTaxid();
		if (StringUtils.isEmpty(taxid)) {
			return false;
		}

		// 测试电话是否正确
		String phoneNum = trader.getPhoneNum();
		if (StringUtils.isNotBlank(phoneNum) && !RegexUtil.isPhoneOrMobile(phoneNum)) {
			return false;
		}

        // TODO: 默认项目的保存 
		// Boolean isDefault = trader.getIsDefault();
		// if (isDefault == null) {
		// 	return ResponseUtil.badArgument();
		// }
		return true;
	}    
}
