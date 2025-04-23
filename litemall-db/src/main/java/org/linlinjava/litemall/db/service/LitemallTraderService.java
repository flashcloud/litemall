package org.linlinjava.litemall.db.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.springframework.transaction.annotation.Transactional;
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
        List<LitemallTrader> traders = new  ArrayList<LitemallTrader>();
        if(traderIds.length == 0){
            return traders;
        }

        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andIdIn(Arrays.asList(traderIds)).andStatusEqualTo(CommonStatusConstant.STATUS_ENABLE).andDeletedEqualTo(false);
        traders = traderMapper.selectByExample(example);

        return traders;
    }

    @Transactional
    public void add(Integer userId, LitemallTrader trader) {
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
                if (trader.getIsDefault() || traderIds.length == 0) {
                    user.setDefaultTraderId(trader.getId());
                }
                userService.updateById(user);
            }
        }

        if (userId > 0) {
            //如果是用户添加的商户，绑定该用户到此商户
            LitemallUser user = userService.findById(userId);
            if (user != null) {
                Integer[] userIds = new Integer[1];
                userIds[0] = user.getId();
                trader.setUserIds(userIds);
            }
        }

        //更新用户绑定的商户
        updateUsersTraders(trader);
    }

    public boolean checkCanEditTrader(Integer userId, Integer traderId) {
        LitemallTrader traderOld = queryById(traderId);
        // 如果userId > 0，则只能更新用户自己创建的商户，或公司的负责人才能更新
        if (!(traderOld.getUserId().equals(userId) || traderOld.getCreatorId().equals(userId))) {
            return false;
        }
        return true;
    }

    @Transactional
    public int updateById(Integer userId, LitemallTrader trader) {
        if (userId > 0) {
            if (!checkCanEditTrader(userId, trader.getId())) {
                return 0;
            }

            //禁止修改商户的负责人和创建人
            LitemallTrader traderOld = queryById(trader.getId());
            trader.setUserId(traderOld.getUserId());
            trader.setCreatorId(traderOld.getCreatorId());
            //禁止修改商户的创建时间
            trader.setAddTime(traderOld.getAddTime());

            // 如果设置了默认商户，更新用户的默认商户
            updateDefaultTrader(userId, trader);
        }

        //如果当前商户设置了负责人，更新负责人的商户列表
        if (trader.getUserId() != null) {
            LitemallUser user = userService.findById(trader.getUserId());
            if (user != null) {
                Integer[] traderIds = user.getTraderIds();
                if (traderIds == null) {
                    traderIds = new Integer[0];
                }
                List<Integer> traderIdList = new ArrayList<>(Arrays.asList(traderIds));
                if (!traderIdList.contains(trader.getId())) {
                    traderIdList.add(trader.getId());
                    traderIds = traderIdList.toArray(new Integer[0]);
                    user.setTraderIds(traderIds);
                    userService.updateById(user);
                }
            }
        }

        //更新用户绑定的商户
        updateUsersTraders(trader);

        if (trader.getUserId() == null) trader.setUserId(0);
        trader.setUpdateTime(LocalDateTime.now());
        return traderMapper.updateByPrimaryKeySelective(trader);
    }

    /**
     * 更新用户绑定的商户
     * @param userId
     * @param trader
     * @param trader
     */
    private void updateUsersTraders(LitemallTrader trader) {
        //编辑后的绑定用户组ID
        Integer[] modifiedUserIds = trader.getUserIds();
        // 原来的绑定用户组ID
        Integer[] oldUserIds = null;
        if (trader.getId() > 0) {
            LitemallTrader traderOld = queryById(trader.getId());
            if (traderOld != null) {
                oldUserIds = usedTraderByUserIds(trader.getId());
            }
        }
        if (oldUserIds == null) {
            oldUserIds = new Integer[0];
        }

        // 通过比较modifiedUserIds和oldUserIds，找出需要添加和删除的用户ID
        List<Integer> addUserIds = new ArrayList<>();
        List<Integer> removeUserIds = new ArrayList<>();
        if (modifiedUserIds != null) {
            for (Integer userId : modifiedUserIds) {
                if (!Arrays.asList(oldUserIds).contains(userId)) {
                    addUserIds.add(userId);
                }
            }
        }
        for (Integer userId : oldUserIds) {
            if (modifiedUserIds == null || !Arrays.asList(modifiedUserIds).contains(userId)) {
                removeUserIds.add(userId);
            }
        }
        // 添加新的用户ID
        if (addUserIds.size() > 0) {
            for (Integer userId : addUserIds) {
                LitemallUser user = userService.findById(userId);
                if (user != null) {
                    Integer[] traderIds = user.getTraderIds();
                    if (traderIds == null) {
                        traderIds = new Integer[0];
                    }
                    List<Integer> traderIdList = new ArrayList<>(Arrays.asList(traderIds));
                    if (!traderIdList.contains(trader.getId())) {
                        traderIdList.add(trader.getId());
                        traderIds = traderIdList.toArray(new Integer[0]);
                        user.setTraderIds(traderIds);
                        userService.updateById(user);
                    }
                }
            }
        }
        // 删除不需要的用户ID
        if (removeUserIds.size() > 0) {
            for (Integer userId : removeUserIds) {
                LitemallUser user = userService.findById(userId);
                if (user != null) {
                    Integer[] traderIds = user.getTraderIds();
                    if (traderIds != null) {
                        List<Integer> traderIdList = new ArrayList<>(Arrays.asList(traderIds));
                        traderIdList.remove(trader.getId());
                        traderIds = traderIdList.toArray(new Integer[0]);
                        user.setTraderIds(traderIds);
                        userService.updateById(user);
                    }
                }
            }
        }
    }

    public void updateDefaultTrader(Integer userId, LitemallTrader trader) {
        LitemallUser user = userService.findById(userId);
        if (user != null) {
            if (trader.getIsDefault()) {
                user.setDefaultTraderId(trader.getId());
                userService.updateById(user);
            }
        }
    }

    @Transactional
    public void deleteById(Integer id) {
        // 如果该商户有用户在使用，不能删除
        if (isTraderUsed(id)) {
            return;
        }

        deleteHlp(0, id);
    }

    /**
     * 如果该商户有用户在使用，不能删除
     * @param loginUserId
     * @param id
     */
    @Transactional
    public void deleteByUser(Integer loginUserId, Integer id) {
        // 如果该商户有用户在使用，不能删除
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

        deleteHlp(loginUserId, id);
    }

/**
     * 获取用户的商户列表
     * @param userId 用户ID
     * @return 商户列表
     */
    public List<LitemallTrader> getTraders(Integer userId) {
        List<LitemallTrader> traders = new  ArrayList<LitemallTrader>();
        if(userId == 0){
            return traders;
        }

        LitemallUser user = userService.findById(userId);
        traders = queryByIds(user.getTraderIds());
        
        // 设置默认商户
        for (LitemallTrader trader : traders) {
            boolean isDefault = trader.getId() == user.getDefaultTraderId();
            trader.setIsDefault(isDefault);
            if (isDefault)  break;
        }

        return traders;
    }

    public LitemallTrader getTrader(Integer userId, Integer traderId) {
        if (traderId == null || traderId == 0 || userId == null || userId == 0) {
            return null;
        }
        List<LitemallTrader> traders = getTraders(userId);
        if(traders == null || traders.size() == 0){
            return null;
        }
        for (LitemallTrader trader : traders) {
            if (trader.getId().equals(traderId)) {
                return trader;
            }
        }
        return null;
    }

    public LitemallTrader getDefaultTrader(Integer userId) {
        if (userId == null || userId == 0) {
            return null;
        }
        List<LitemallTrader> traders = getTraders(userId);
        if(traders == null || traders.size() == 0){
            return null;
        }
        for (LitemallTrader trader : traders) {
            if (trader.getIsDefault()) {
                return trader;
            }
        }
        return null;
    }

    private void deleteHlp(Integer userId, Integer id) {
        //如果当前商户有订单，不能删除
        if (orderService.countByTrader(id) > 0)  return;

         //将该商户的的税号后面更新为随机字符串
        LitemallTrader trader = queryById(id);
        if (trader == null) {
            return;
        }
        trader.setTaxid(trader.getTaxid() + "--@@--" + RandomStringUtils.randomAlphanumeric(20));
        updateById(userId, trader);
        traderMapper.logicalDeleteByPrimaryKey(id);

        //删除商户后，查找所有默认商户是该商户的所有用户，把它们的默认商户设置为0
        resetDefaultTrader(trader);
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
                sb.append(userService.getUserFullName(user.getId()));
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

    private void resetDefaultTrader(LitemallTrader trader) {
        if (trader == null) {
            return;
        }
        List<LitemallUser> users = userService.queryAllByDefaultTrader(trader);
        for (LitemallUser user : users) {
            List<LitemallTrader> traders =  getTraders(user.getId());
            if (traders == null || traders.size() == 0) {
                user.setDefaultTraderId(0);
            } else  {
                user.setDefaultTraderId(traders.get(0).getId());
            }
            userService.updateById(user);
        }
    }
}
