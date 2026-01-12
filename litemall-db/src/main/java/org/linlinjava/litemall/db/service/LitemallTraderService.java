package org.linlinjava.litemall.db.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.linlinjava.litemall.db.dao.LitemallOrderGoodsMapper;
import org.linlinjava.litemall.db.dao.LitemallTraderMapper;
import org.linlinjava.litemall.db.dao.OrderMapper;
import org.linlinjava.litemall.db.domain.LitemallTraderExample;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;
import org.linlinjava.litemall.db.util.CommonStatusConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.linlinjava.litemall.db.domain.LitemallOrderExample;
import org.linlinjava.litemall.db.domain.LitemallOrderGoods;
import org.linlinjava.litemall.db.domain.LitemallOrderGoodsExample;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.pagehelper.PageHelper;

@Service
public class LitemallTraderService {
    private final Log logger = LogFactory.getLog(LitemallTraderService.class);

    @Autowired
    private Environment environment;

    @Autowired
    private LitemallUserService userService;

    @Autowired
    private LitemallOrderService orderService;

    @Autowired
    private LitemallOrderGoodsService orderGoodsService;

    @Resource
    private LitemallTraderMapper traderMapper;
   @Resource
    private OrderMapper orderMapper;
    @Resource
    private LitemallOrderGoodsMapper orderGoodsMapper;
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

    public LitemallTrader queryByShareCode(String shareCode) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andShareCodeEqualTo(shareCode).andDeletedEqualTo(false);
        return traderMapper.selectOneByExample(example);
    }    

    // 根据用户获取该用户管理的交易商户列表
    public List<LitemallTrader> managedByUser(LitemallUser user) {
        // 创建一个空的交易商户列表
        List<LitemallTrader> traders = new  ArrayList<LitemallTrader>();
        // 如果用户ID为空或为0，则返回空列表
        if(user.getId() == null || user.getId() == 0){
            return traders;
        }

        // 创建一个交易商户示例
        LitemallTraderExample example = new LitemallTraderExample();
        // 设置示例的条件，用户ID等于传入的用户ID，状态为启用，未删除
        example.or().andUserIdEqualTo(user.getId()).andStatusEqualTo(CommonStatusConstant.STATUS_ENABLE).andDeletedEqualTo(false);
        // 根据示例查询交易商户列表
        traders = traderMapper.selectByExample(example);

        // 返回交易商户列表
        return traders;
    }

    @Transactional
    public int add(Integer userId, LitemallTrader trader) {
        trader.setAddTime(LocalDateTime.now());
        trader.setUpdateTime(LocalDateTime.now());
        traderMapper.insertSelective(trader);
        int traderId = trader.getId();
        
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

        return traderId;
    }

    /**
     *  添加用户.并将该用户与KEY对应的交易商户绑定
     * @param user
     * @param dogKey 用户绑定的key
     */
    @Transactional
    public boolean boundTrader(LitemallUser user, String dogKey) {
        if (!checkRegisterUser(dogKey)) return false;

        //根据KEY找到对应的订单明细，再根据订单明细找到对应的订单，从该订单中找到交易商户，将该用户和交易商户绑定
        TraderOrderGoodsVo orderedGoods = orderService.getTraderOrderedPCAppBySerial(null, dogKey); //TODO:serial
        LitemallTrader trader = queryById(orderedGoods.getTraderId());

        //userService.add(user);

        registerUserHelp(user, trader);

        // 更新订单的hasRegisterUserIds
        LitemallOrderGoods orderedGoodsOld = orderGoodsService.findById(orderedGoods.getId());
        this.orderService.boundUserToPCAppOrderGoods(user.getId(), orderedGoodsOld);

        return true;
    }

    public String share(LitemallUser user, Integer traderId) {
        Integer userId = user.getId();
        if (!isManagedByUser(user, traderId)) return "";
        
		LitemallTrader trader = queryById(traderId);
		if (trader == null) {
			return "";
		}
        
        String shareCode = UUID.randomUUID().toString();
        trader.setShareCode(shareCode);
        traderMapper.updateByPrimaryKeySelective(trader);

        return trader.getShareCode();
    }

    @Transactional
    public LitemallTrader registerUser(LitemallUser user, LitemallTrader trader) {
        LitemallTrader  dbTrader = null;
        if (trader.getId() == null || trader.getId() == 0) {
            dbTrader = existsInDBTrader(trader);
            
            if (dbTrader == null) {
                int traderId = add(user.getId(), trader);
                dbTrader = queryById(traderId);
            }
        } else {
            dbTrader = queryById(trader.getId());
        }

        registerUserHelp(user, dbTrader);

        return  dbTrader;
    }

    public Object boundTraderByShare(LitemallUser user, String shareCode) {
        LitemallTrader trader = existsInDBTrader(shareCode);
        return boundTraderByHelp(user, trader);
    }

    public Object boundTraderBySoftDevTrader(LitemallUser user) {
        LitemallTrader trader = getSoftwareDevTrader();
        return boundTraderByHelp(user, trader);
    }

    private Object boundTraderByHelp(LitemallUser user, LitemallTrader trader) {
        if (trader == null) {
            return 1;
        }

        if (trader.getUserId() == user.getId() || isTraderOfUser(user.getId(), trader.getId())) {
            return 2;
        }

        trader = registerUser(user, trader);

        //绑定成功后，清除分享码
        trader.setShareCode("");
        traderMapper.updateByPrimaryKeySelective(trader);

        return trader;
        
    }    

    /**
     * 查找数据库中是否存在相同的商户，根据税号或名称查找，税号优先
     * @param trader
     * @return
     */
    public LitemallTrader existsInDBTrader(LitemallTrader trader) {
        LitemallTrader  dbTrader =  queryByTaxCode(trader.getTaxid());
        return dbTrader != null ? dbTrader : queryByName(trader.getCompanyName());
    }

    public LitemallTrader existsInDBTrader(String shareCode) {
        LitemallTrader  dbTrader =  queryByShareCode(shareCode);
        return dbTrader;
    }    

    @Transactional
    public void registerUserHelp(LitemallUser user, LitemallTrader trader) {
        //如果该商户没有负责人，设置当前用户为负责人和默认商户
        if (trader.getUserId() == null || trader.getUserId() == 0 || userService.findById(trader.getUserId()) == null) {
            trader.setUserId(user.getId());
            trader.setIsDefault(true);
            traderMapper.updateByPrimaryKey(trader);
        }
        
        //绑定用户和交易商户
        user.setDefaultTraderId(trader.getId());
        user.setTraderIds(new Integer[]{trader.getId()});
        userService.updateById(user);
    }

    public boolean unregisterUser(LitemallUser user, LitemallTrader trader) {
        if ((trader.getUserId() != null && trader.getUserId().equals(user.getId())) || (trader.getCreatorId() != null && trader.getCreatorId().equals(user.getId()))) {
            // 负责人或创建人不能解除绑定
            return false;
        }

        //解除用户和交易商户的绑定
        Integer[] traderIds = user.getTraderIds();
        if (traderIds != null) {
            List<Integer> traderIdList = new ArrayList<>(Arrays.asList(traderIds));
            traderIdList.remove(trader.getId());
            traderIds = traderIdList.toArray(new Integer[0]);
            user.setTraderIds(traderIds);
            if (user.getDefaultTraderId() != null && user.getDefaultTraderId().equals(trader.getId())) {
                user.setDefaultTraderId(0);
            }
            userService.updateById(user);

            return true;
        }

        return false;
    }

    /**
     *  检查用户是否可以注册
     * @param user
     * @param dogKey
     * @return
     */
    public boolean checkRegisterUser(String dogKey) {
        TraderOrderGoodsVo orderedGoods = orderService.getTraderOrderedPCAppBySerial(null,dogKey);//这时用户还未注册，所以不能传入用户ID
        if (orderedGoods == null) return false;

        LitemallTrader trader = queryById(orderedGoods.getTraderId());
        if (trader == null) return false;

        return orderedGoods.getMaxRegisterUsersCount() > orderedGoods.getHasRegisterUserIds().length;
        
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
            trader.setUserIds(new Integer[]{userId}); // 保持绑定的用户ID不变
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

        // 把用户自己创建的商户也加进去
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andUserIdEqualTo(userId).andDeletedEqualTo(false);
        List<LitemallTrader> responseTraders = traderMapper.selectByExample(example);
        for (LitemallTrader trader : responseTraders) {
            if (!traders.contains(trader)) {
                traders.add(trader);
            }
        }
        
        // 设置默认商户
        for (LitemallTrader trader : traders) {
            boolean isDefault = trader.getId() == user.getDefaultTraderId();
            trader.setIsDefault(isDefault);
            if (isDefault)  break;
        }

        return traders;
    }

    /**
     * 判断商户是否被指定的用户管理
     * @param user
     * @param traderId
     * @return
     */
    public boolean isManagedByUser(LitemallUser user, Integer traderId) {
        if (user == null || traderId == null) {
            return false;
        }
        List<LitemallTrader> traders = managedByUser(user);
        if (traders == null || traders.size() == 0) {
            return false;
        }
        for (LitemallTrader trader : traders) {
            if (traderId.equals(trader.getId())) {
                return true;
            }
        }
        return false;
    }

    // 指定的商户ID是否是指定的用户的商户
    public boolean isTraderOfUser(Integer userId, Integer traderId) {
        return getTrader(userId, traderId) != null;
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

    /**
     * 获取软件开发交易商户
     * @return
     */
    public LitemallTrader getSoftwareDevTrader() {
        String taxid = environment.getProperty("litemall.core.soft-dev-info.tax-id");
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andTaxidEqualTo(taxid).andDeletedEqualTo(false);
        return traderMapper.selectOneByExample(example);
    }    

    private void deleteHlp(Integer userId, Integer traderId) {
        LitemallTrader developer = getSoftwareDevTrader();
        if (developer != null && developer.getId().equals(traderId)) {
            return;
        }
        //如果当前商户有订单，不能删除
        if (orderService.countByTrader(traderId) > 0)  return;

         //将该商户的的税号后面更新为随机字符串
        LitemallTrader trader = queryById(traderId);
        if (trader == null) {
            return;
        }
        trader.setTaxid(trader.getTaxid() + "--@@--" + RandomStringUtils.randomAlphanumeric(20));
        updateById(userId, trader);
        traderMapper.logicalDeleteByPrimaryKey(traderId);

        //删除商户后，查找所有默认商户是该商户的所有用户，把它们的默认商户设置为0
        resetDefaultTrader(trader);
    }

    /**
     * 检查商户是否被使用
     * @param id
     * @return
     */
    public boolean isTraderUsed(Integer traderId) {
        return isTraderUsedHlp(0, traderId);
    }

    public boolean isTraderUsedByOtherUsers(Integer userId, Integer traderId) {
        return isTraderUsedHlp(userId, traderId);
    }
    
    /**
     * 使用了指定商户的所有用户
     * @param traderId
     * @return
     */
    public List<LitemallUser> usedTraderByUsers(Integer traderId) {
        return usedTraderByUsersHlp(0, traderId);
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
     * @param userId 如果userId不为空，则返回除了该用户以外的其他用户
     * @param traderId
     * @return
     */
    public List<LitemallUser> usedTraderByOtherUsers(Integer userId, Integer traderId) {
        return usedTraderByUsersHlp(userId, traderId);
    }

    /**
     * 获取使用了指定商户的其他用户的数量
     * @param userId 如果userId不为空，则返回除了该用户以外的其他用户
     * @param traderId
     * @return
     */
    public Integer usedTraderByOtherUsersCount(Integer userId, Integer traderId) {
        List<LitemallUser> userList =  usedTraderByOtherUsers(userId, traderId);
        return userList == null ? 0 : userList.size();
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
    
    private List<LitemallUser> usedTraderByUsersHlp(Integer userId, Integer traderId) {
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
                for (Integer id : traderIds) {
                    if (id.equals(traderId)) {
                        usedUsers.add(user);
                    }
                }
            }
        }
        return new ArrayList<>(usedUsers);
    }

    private boolean isTraderUsedHlp(Integer userId, Integer traderId) {
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
                for (Integer id : traderIds) {
                    if (id.equals(traderId)) {
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

    public LitemallTrader queryByName(String name) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andCompanyNameEqualTo(name).andDeletedEqualTo(false);
        List<LitemallTrader> list = traderMapper.selectByExample(example);
        return list != null && list.size() > 0 ? list.get(0) : null;
    }

    public LitemallTrader queryByTaxCode(String taxCode) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andTaxidEqualTo(taxCode).andDeletedEqualTo(false);
        List<LitemallTrader> list = traderMapper.selectByExample(example);
        return list != null && list.size() > 0 ? list.get(0) : null;
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
			nickname = companyName;
            trader.setNickname(nickname);
		}
        String taxid = trader.getTaxid();
		if (StringUtils.isEmpty(taxid)) {
			return false;
		}

        // TODO: 默认项目的保存 
		// Boolean isDefault = trader.getIsDefault();
		// if (isDefault == null) {
		// 	return ResponseUtil.badArgument();
		// }
		return true;
	}

    /**
     * 注册用户，并为试用用户绑定软件开发商户和添加试用会员订单
     * @param user
     * @param isTrial
     * @return
     */
    @Transactional
    public Object registerUserForController(LitemallUser user, boolean isTrial) {
        Integer userId = userService.add(user);
        if (!isTrial) return userId;

        // 绑定软件开发商户
        Object boundRet = boundTraderBySoftDevTrader(user);
        if (boundRet instanceof LitemallTrader) {
            // 添加试用会员订单
            LitemallOrderGoodsExample example = new LitemallOrderGoodsExample();
            example.or().andIdEqualTo(4).andDeletedEqualTo(false); //会员要试用的商品软件固定为4, 在系统初始化时添加：./litemall-db/sql/litemall_init.sql
            List<LitemallOrderGoods> orderGoodsList = orderGoodsMapper.selectByExample(example);
            if (orderGoodsList.size() == 0) {
                logger.error("试用用户注册时，添加免费会员失败，找不到订单商品数据，订单商品ID=4");
                return userId;
            }
            LitemallOrderGoods udiNetSoftwareOrderGoods = orderGoodsList.get(0);
            String key = udiNetSoftwareOrderGoods.getSerial(); // 软件授权KEY
            String newOrderSn = orderService.generateOrderSn(userId); // 生成新的订单号
            LocalDateTime expirDateTime =   LocalDateTime.now().plusDays(7); //试用7天
            orderMapper.insertTrialMember(userId, user.getMobile(), newOrderSn); // 插入试用会员订单
            int newMemberOrderId = orderService.findBySn(newOrderSn).getId();
            orderMapper.insertTrialMemberGoods(newMemberOrderId, key, expirDateTime); // 插入试用会员订单商品

            // 将会员订单ID保存到用户表的memberOrderId字段
            user.setMemberOrderIds(new Integer[]{newMemberOrderId});
            userService.updateById(user);

            // 将新注册的用户ID保存到软件订单的已注册用户ID列表
            Integer[] hasRegUserIds = Arrays.copyOf(udiNetSoftwareOrderGoods.getHasRegisterUserIds(), udiNetSoftwareOrderGoods.getHasRegisterUserIds().length + 1);
            hasRegUserIds[udiNetSoftwareOrderGoods.getHasRegisterUserIds().length] = userId;
            udiNetSoftwareOrderGoods.setHasRegisterUserIds(hasRegUserIds);
            orderGoodsService.updateById(udiNetSoftwareOrderGoods);
        }
        
        //返回新注册的用户ID
        return userId;
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
