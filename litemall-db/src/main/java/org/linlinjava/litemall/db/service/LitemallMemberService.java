package org.linlinjava.litemall.db.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.db.dao.LitemallGoodsMapper;
import org.linlinjava.litemall.db.dao.OrderMapper;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.domain.LitemallGoodsExample;
import org.linlinjava.litemall.db.domain.LitemallGoodsSpecification;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.LitemallOrderGoods;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.MemberType;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;
import org.linlinjava.litemall.db.exception.DataStatusException;
import org.linlinjava.litemall.db.exception.MaxTwoMemberOrderException;
import org.linlinjava.litemall.db.exception.MemberOrderDataException;
import org.linlinjava.litemall.db.exception.MemberStatusException;
import org.linlinjava.litemall.db.util.CommonStatusConstant;
import org.linlinjava.litemall.db.util.DbUtil;
import org.linlinjava.litemall.db.util.KeywordsConstant;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;

@Service
public class LitemallMemberService {
    private final Log logger = LogFactory.getLog(LitemallMemberService.class);

    public static enum MemberStatus {
        MEMBER_ISNOT,   // 非会员
        MEMBER_NOT_EXIST, // 会员不存在
        MEMBER_NOT_PAID, //会员未支付
        MEMBER_UNBOUND, //会员未绑定商户
        MEMBER_EXPIRED, //会员已过期
        MEMBER_ACTIVE, //会员正常
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Resource
    private LitemallGoodsMapper goodsMapper;
    @Resource
    private OrderMapper orderMapper;
    
    @Autowired
    private LitemallUserService userService;
    @Autowired
    private LitemallGoodsService goodsService;
    @Autowired
    private LitemallOrderService orderService;
    @Autowired
    private LitemallOrderGoodsService orderGoodsService;
    @Autowired
    private LitemallGoodsProductService productService;

    /**
     * 判断订单是否包含会员商品
     * @param orderId
     * @return
     */
    public boolean isMemberOrder(Integer orderId) {
        LitemallOrderGoods memberOrderGoods = queryMemberOrderGoods(orderId);
        return memberOrderGoods != null;
    }    

    public boolean isMemberGoods(LitemallGoods goods) {
        if (goods == null) return false;
        if (goods.getGoodsType() == LitemallGoods.GoodsType.MEMBER) return true;
        return false;
    }

    /**
     * 获取指定用户未确认收款但已支付的会员订单
     * @param userId
     * @return
     */
    public List<TraderOrderGoodsVo> getUserNoCheckedButPayedMemberOrders(Integer userId, Integer pcAppOrderId) {
        List<TraderOrderGoodsVo> result = orderMapper.getUserNoCheckedButPayedMember(userId , pcAppOrderId);
        return result;
    }    
    
    /**
     * 获取会员商品
     * 会员商品是指：关键词为SYS-MEMBER的商品
     * @param offset
     * @param limit
     * @return
     */
    public List<LitemallGoods> queryByUserMember(int offset, int limit) {
        LitemallGoodsExample example = new LitemallGoodsExample();
        example.or().andKeywordsLike(KeywordsConstant.KEYWORDS_MEMBER + '%').andDeletedEqualTo(false);
        example.setOrderByClause("add_time desc");
        PageHelper.startPage(offset, limit);

        return goodsMapper.selectByExampleSelective(example, goodsService.columns);
    }

    public boolean checkMemberStatus(LitemallUser user) throws IllegalArgumentException, MemberOrderDataException, DataStatusException {
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Integer[] memberOrderIds = user.getMemberOrderIds();
        if (memberOrderIds == null || memberOrderIds.length == 0) {
            // 用户没有会员订单
            return false;
        }

        for (Integer memberOrderId : user.getMemberOrderIds()) { 
            if (checkMemberStatus(user, memberOrderId) != MemberStatus.MEMBER_ACTIVE) {
                return false;
            }
        }
        return true;
    }

    public MemberStatus checkMemberStatus(LitemallUser user , String dogKey) throws IllegalArgumentException, MemberOrderDataException, DataStatusException {
        if (user.getTraderIds().length == 0) return MemberStatus.MEMBER_UNBOUND; //如果没有绑定的商户，则返回false
        TraderOrderGoodsVo traderOrderGoodsVo = orderService.getTraderOrderedPCAppBySerial(user, dogKey);
        if (traderOrderGoodsVo == null) return MemberStatus.MEMBER_NOT_EXIST;
        return checkMemberStatus(user, traderOrderGoodsVo.getOrderId());
    }

    /**
     * 登录时检查用户的会员状态
     * @param user
     * @return 如果检查通过，返回true；否则返回false
     * @throws MemberOrderDataException
     * @throws DataStatusException
     */
    public MemberStatus checkMemberStatus(LitemallUser user, int memberOrderId) throws IllegalArgumentException, MemberOrderDataException, DataStatusException {
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() != CommonStatusConstant.STATUS_ENABLE) {
            throw new IllegalArgumentException("用户已被禁用");
        }

        if (!user.isMemberOrderIdExists(memberOrderId)) return MemberStatus.MEMBER_ISNOT;
        
        LitemallOrder memberOrder = orderService.findById(user.getId(), memberOrderId);
        if (memberOrder == null) throw new DataStatusException("会员订单不存在");
        if (!memberOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM)) throw new DataStatusException("会员订单状态异常");

        LocalDateTime now = LocalDateTime.now();
        
        //验证会员订单上的数据与用户表上的会员数据是否一致
        LitemallOrderGoods memberOrderGoods = queryMemberOrderGoods(memberOrderId);
        LitemallGoodsSpecification memberSpeci = queryMemberGoodsSpecification(memberOrder);
        if (memberOrderGoods == null || memberSpeci == null)  throw new DataStatusException("会员订单数据异常");
        if (memberOrderGoods.getExpDate() == null)  throw new DataStatusException("会员的效期数据异常");

        LocalDateTime memberExpire = memberOrderGoods.getExpDate();
        if (now.isBefore(memberExpire)) return MemberStatus.MEMBER_ACTIVE; //会员未过期

        if (memberOrder.getParentOrderId() == null || memberOrder.getParentOrderId() <= 0) return MemberStatus.MEMBER_EXPIRED; //没有上级会员订单,并且已过期
        
        // 用户会员已过期，则看是否存在需要延期的会员订单
        LitemallOrder parentOrder = orderService.findById(user.getId(), memberOrder.getParentOrderId());
        if (parentOrder == null) throw new DataStatusException("上级会员订单不存在");
        if (!(parentOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM))) throw new DataStatusException("上级会员订单状态异常");

        LitemallOrderGoods parentMemberOrderGoods = queryMemberOrderGoods(parentOrder.getId());
        LitemallGoodsSpecification parentMemberSpeci = queryMemberGoodsSpecification(parentOrder);
        if (parentMemberOrderGoods == null || parentMemberOrderGoods.getExpDate() == null) throw new DataStatusException("上级会员订单数据异常");

        LocalDateTime parentExpire = parentMemberOrderGoods.getExpDate();
        if (now.isBefore(parentExpire)) {
            parentOrder.setParentOrderId(0);
            orderService.updateSelective(parentOrder);
            //上级会员未过期，则将用户的关联的会员重置为上级会员订单
            //更新用户的会员信息
            updateMemberAttData(user, parentOrder);
            return MemberStatus.MEMBER_ACTIVE; //上级会员未过期，则用户的会员状态为有效
        } else {
            //上级会员已过期,则放弃上级会员，减少会员状态查找的性能
            memberOrder.setParentOrderId(0); //如果上级会员订单已经过期，则将旧订单的上级会员订单重置
            orderService.updateSelective(memberOrder);
            return MemberStatus.MEMBER_EXPIRED; //上级会员已过期，则用户的会员状态为过期
        }
    }    

    /**
     * 检查会员商品数据
     * @param memberGoodsList
     * @return
     * @throws MemberOrderDataException 
     */
    public boolean  checkMemberGoodsData(Integer totalItemLength, List<LitemallGoods> memberGoodsList) throws IllegalArgumentException,MemberOrderDataException {
        if (totalItemLength < 0 || totalItemLength < memberGoodsList.size()) {
            throw new IllegalArgumentException("totalItemLength参数不合法");
        }
        if (memberGoodsList.size() > 1) {
            throw new MemberOrderDataException("一次只能购买一种会员商品");
        } else if (memberGoodsList.size() == 1) {
            // 如果订单中有会员商品，则只能购买一个会员商品
            LitemallGoods memberGoods = memberGoodsList.get(0);
            if (memberGoods.getNumber() != 1) {
                throw new MemberOrderDataException("会员商品数量必须为1");
            }
            if (totalItemLength > 1) {
                throw new MemberOrderDataException("会员商品不能与普通商品一起购买，请单独下单");
            }
        }
        return true;
    }    


    /** 检查用户是否可以购买会员 
     * @throws MemberStatusException */
    public boolean checkMemberCanPurchase(LitemallUser user, Integer pcAppOrderId) throws MemberStatusException {
        Integer[] memberOrderIds = user.getMemberOrderIds();
        if (memberOrderIds == null || memberOrderIds.length == 0) return true;

        // 获取指定软件订单rootOrderId对应的最新会员订单
         Integer activeMemberOrderId = 0;
        for (Integer memberOrderId : memberOrderIds) {
            LitemallOrder memberOrder = orderService.findById(user.getId(), memberOrderId);
            if (memberOrder == null) continue;
            if (!memberOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM)) continue;
            if (memberOrder.getRootOrderId() == null || memberOrder.getRootOrderId() <= 0) continue;
            if (memberOrder.getRootOrderId().equals(pcAppOrderId)) {
                activeMemberOrderId = memberOrder.getId();
                break;
            }
        }
        if (activeMemberOrderId == 0) return true;
        LitemallOrder existsMemberOrder = orderService.findById(user.getId(), activeMemberOrderId);
        if (existsMemberOrder == null) return true;
        
        LitemallOrderGoods activeMember = queryMemberOrderGoods(activeMemberOrderId);
        if (activeMember == null || activeMember.getExpDate() == null) return true;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime memberExpire = activeMember.getExpDate();
        if (now.isAfter(memberExpire)) return true; //会员过期

        //如果会员未过期，则检查他的父级会员订单，是否存在未过期的订单
        if (existsMemberOrder.getParentOrderId() == null || existsMemberOrder.getParentOrderId() == 0) return true;
        LitemallOrder parentOrder = orderService.findById(user.getId(), existsMemberOrder.getParentOrderId());
        if (parentOrder == null) return true;
        if (!(parentOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM))) return true;
        LitemallOrderGoods parentMemberGoods = queryMemberOrderGoods(parentOrder.getId());
        LocalDateTime parentExpire = parentMemberGoods.getExpDate();
        if (parentMemberGoods == null || parentExpire == null) return true;
        if (now.isAfter(parentExpire)) {
            //父订单必须是过期的才能购买新的会员
            return true;
        } else {
            throw new MemberStatusException("请在本期会员订阅到期后再续订!");
        }
    }

    public LitemallOrderGoods queryMemberOrderGoods(Integer orderId) {
        List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(orderId);
        for (LitemallOrderGoods orderGoods : orderGoodsList) {
            LitemallGoods goods = goodsService.findById(orderGoods.getGoodsId());
            goods.setNumber(orderGoods.getNumber());
            orderGoods.setGoodsType(goods.getGoodsType());
            if (isMemberGoods(goods)) {
                return orderGoods; // 返回第一个会员商品
            }
        }
        return null;
    }

    public LitemallGoodsSpecification queryMemberGoodsSpecification(LitemallOrder newOrder) throws IllegalArgumentException, MemberOrderDataException {
        List<LitemallGoods> memberGoodsList = new ArrayList<>();
        LitemallOrderGoods newMemberOrderGoods = null;
        List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(newOrder.getId());
        //如果订单中的商品是会员套餐，则设置用户的会员状态
        if (newOrder.getOrderStatus().equals(OrderUtil.STATUS_PAY) || newOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM)) {
            for (LitemallOrderGoods orderGoods : orderGoodsList) {
                LitemallGoods goods = goodsService.findById(orderGoods.getGoodsId());
                goods.setNumber(orderGoods.getNumber());
                orderGoods.setGoodsType(goods.getGoodsType());
                if (goods.getGoodsType() == LitemallGoods.GoodsType.MEMBER) {
                    memberGoodsList.add(goods);
                    newMemberOrderGoods = orderGoods;
                }
            }
        }

        if (newMemberOrderGoods == null) return null;

        if (!checkMemberGoodsData(orderGoodsList.size(), memberGoodsList)) return null;

        List<LitemallGoodsSpecification> newMemberSpeciList = productService.findByProduct(productService.findById(newMemberOrderGoods.getProductId()));
        if (newMemberSpeciList.size() > 0) {
            return newMemberSpeciList.get(0);
        }
        return null;
    }


    /**
     * 如果订单是会员商品，则更新用户的会员状态
     * @param newOrder
     * @param memberOrderGoods
     * @return boolean 如果是会员订单，则返回true
     * @throws MemberOrderDataException | MaxTwoMemberOrderException | IllegalArgumentException
     */
    public boolean updateUserMemberStatus(LitemallOrder newOrder) throws IllegalArgumentException, MemberOrderDataException, MaxTwoMemberOrderException {
        if (newOrder == null) return false;

        if (!isMemberOrder(newOrder.getId())) return false;

        LitemallUser user = userService.findById(newOrder.getUserId());
        if (user == null) return false;

        //会员的启用日期以订单支付时间为准
        LocalDateTime newMemberStartDate = newOrder.getPayTime();

        // 计算新的会员到期日和会员套餐
        LocalDateTime newMemberExpDate = null;
        String newMemberPlan = "";
        LitemallGoodsSpecification newMemberSpeci = queryMemberGoodsSpecification(newOrder);
        if (newMemberSpeci != null) {
            int days = newMemberSpeci.getSpecificationType().getDays();
            newMemberExpDate = newMemberStartDate.plusDays(days);
            newMemberPlan = newMemberSpeci.getKeywords();
        }

        //如果会员订单已经到期，则忽略此订单
        if (newMemberExpDate != null && newMemberExpDate.isBefore(LocalDateTime.now())) return false;

        Integer[] memberOrderIds = user.getMemberOrderIds();
        if (memberOrderIds != null && memberOrderIds.length > 0) {
            // 获取已有的会员订单
            LitemallOrder oldMemberOrder = this.existsMemberOrderInUsOrder(user, newOrder);
            // 如果之前用户已经存在有会员订单，并且旧的会员订单未到期，则保留旧会员订单的还剩下的天数，以便此新会员订单到期后，能自动恢复延续旧会员订单剩下的天数
            if (oldMemberOrder != null && oldMemberOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM)) {
                Integer oldMemberOrderId = oldMemberOrder.getId();
                LitemallOrderGoods oldMemberGoods = queryMemberOrderGoods(oldMemberOrderId);
                LocalDateTime oldExpDate = oldMemberGoods.getExpDate();
                if (LocalDateTime.now().isBefore(oldExpDate)) {
                    //旧会员订单剩下的天数
                    Integer remainDays = (int) (oldExpDate.toLocalDate().toEpochDay() - LocalDate.now().toEpochDay());
                    LocalDateTime oldNewExpDate = newMemberExpDate.plusDays(remainDays);
                    if (oldNewExpDate.isAfter(oldExpDate)) {
                        //如果旧订单仍然存在未到期的上级会员订单，则禁止购买此次会员
                        if (oldMemberOrder.getParentOrderId() > 0) {
                            LitemallOrder oldMemberParentOrder = orderService.findById(user.getId(), oldMemberOrder.getParentOrderId());
                            if (oldMemberParentOrder != null && oldMemberParentOrder.getOrderStatus().equals(OrderUtil.STATUS_AUTO_CONFIRM)) {
                                LitemallOrderGoods oldMemberParentGoods = queryMemberOrderGoods(oldMemberOrderId);
                                LocalDateTime oldParentExpDate = oldMemberParentGoods.getExpDate();  
                                if (LocalDateTime.now().isBefore(oldParentExpDate)) {
                                    throw new MaxTwoMemberOrderException("用户存在未到期的会员订单，不能购买新的会员");
                                } else {
                                    //如果旧订单的上级会员订单已经到期，则将旧订单的上级会员订单重置
                                    oldMemberOrder.setParentOrderId(0);
                                }
                            }
                        }

                        oldMemberGoods.setExpDate(oldNewExpDate);
                        orderGoodsService.updateById(oldMemberGoods);
                        oldMemberOrder.setUpdateTime(LocalDateTime.now());
                        orderService.updateSelective(oldMemberOrder);

                        //更改新会员订单的父订单为上一次的会员订单，以便新订单的会员到期后，可以自动延期上一个会员订单的到期日
                        newOrder.setParentOrderId(oldMemberOrder.getId());
                        orderService.updateSelective(newOrder);
                    }
                }
            }
        }
        //设置新的会员到期日和设置当前订单会员商品的效期
        if (newMemberExpDate != null) {
            Integer memberOrderId = newOrder.getId();

            LitemallOrderGoods newMemberGoods = queryMemberOrderGoods(memberOrderId);
            
            //更新用户的会员信息
            updateMemberAttData(user, newOrder);

            //如果商户只有一个软件授权订单，则将此订单绑定到用户
            List<TraderOrderGoodsVo> traderOrderGoodsList = orderService.getTraderOrderedPCAppByTrader(newOrder.getTraderId());
            //支持用户选择绑定的软件授权订单
            if (traderOrderGoodsList.size() == 1 || newOrder.getRootOrderId() > 0) {
                LitemallOrderGoods pcOrderGoods = null;
                if (traderOrderGoodsList.size() == 1) {
                    TraderOrderGoodsVo traderOrderGoodsVo = traderOrderGoodsList.get(0);
                    pcOrderGoods = orderGoodsService.findById(traderOrderGoodsVo.getId());
                }else  if (newOrder.getRootOrderId() > 0) {
                    List<LitemallOrderGoods> pcOrderGoodsList = orderGoodsService.queryByOid(newOrder.getRootOrderId());
                    if (pcOrderGoodsList != null && pcOrderGoodsList.size() > 0) {
                        if (pcOrderGoodsList.size() > 1) {
                            throw new IllegalArgumentException("绑定的软件订单中有多套软件，暂不支持通行卡的绑定");
                        }
                        pcOrderGoods = pcOrderGoodsList.get(0);
                    }
                }
                
                if (orderService.boundUserToPCAppOrderGoods(user.getId(), pcOrderGoods)) {
                    //如果是新绑定，则增加对应的授权软件订单的最大注册用户数
                    pcOrderGoods.setMaxRegisterUsersCount((short) (pcOrderGoods.getMaxRegisterUsersCount() + 1));
                    orderGoodsService.updateById(pcOrderGoods);

                    //设置新会员订单的绑定软件授权订单的序列号
                    newMemberGoods.setBoundSerial(pcOrderGoods.getSerial());
                    //将此会员订单的根级订单设置为PC应用软件订单，以便会员订单到期后，可以自动解绑软件授权
                    if (newOrder.getRootOrderId() == 0) {
                        newOrder.setRootOrderId(pcOrderGoods.getOrderId());
                    }
                }
            }

            //设置新会员订单的会员商品效期
            newMemberGoods.setExpDate(newMemberExpDate);
            orderGoodsService.updateById(newMemberGoods);
        }

        //更新订单状态为自动确认，跳过发货和用户手动确认状态
        newOrder.setConfirmTime(LocalDateTime.now());
        newOrder.setOrderStatus(OrderUtil.STATUS_AUTO_CONFIRM);
        orderService.updateSelective(newOrder);

        return true;
    }

    /**
     * 更新用户的会员相关属性值
     * @param user
     * @param memberType
     * @param memberOrderId
     * @param memberPlan
     * @param expireTime
     */
    public void updateMemberAttData(LitemallUser user, LitemallOrder memberOrder) {
        boolean reqUpdate = false;
        Integer memberOrderId = memberOrder.getId();

        Integer[] memberOrderIds = user.getMemberOrderIds();
        if (memberOrderIds == null) {
            memberOrderIds = new Integer[0];
        }
        List<Integer> memberOrderIdList = new ArrayList<>(Arrays.asList(memberOrderIds));
        
        LitemallOrder oldMemberOrder = existsMemberOrderInUsOrder(user, memberOrder);
        if (oldMemberOrder != null) { 
            memberOrderIdList.remove(oldMemberOrder.getId());
            reqUpdate = true;
        }

        if (!memberOrderIdList.contains(memberOrderId)) {
            memberOrderIdList.add(memberOrderId);
            memberOrderIds = memberOrderIdList.toArray(new Integer[0]);
            user.setMemberOrderIds(memberOrderIds);
            reqUpdate = true;
        }

        if (reqUpdate) {
            userService.updateById(user);
        }
    }

    /**
     * 在现有的用户会员订单嘎哥中，找到与指定otherMemberOrder相同软件订单的会员订单
     * @param user
     * @param otherMemberOrder
     * @return
     */
    private LitemallOrder existsMemberOrderInUsOrder(LitemallUser user, LitemallOrder otherMemberOrder) { 
        if (user.getMemberOrderIds() == null) return null;

        for(Integer memberOrderId : user.getMemberOrderIds()) {
            if (memberOrderId.equals(otherMemberOrder.getId())) {
                return otherMemberOrder;
            } else {
                LitemallOrder memberOrder = orderService.findById(user.getId(), memberOrderId);
                if (memberOrder != null) {
                    if (memberOrder.getRootOrderId() == otherMemberOrder.getRootOrderId()) {
                        return memberOrder;
                    }
                }
            }
        }

        return null;
    }

    public String getUserMemberType(LitemallUser user) {
        //获取用户的会员类型的key值
        Integer[] memberOrderIds = user.getMemberOrderIds();
        if (memberOrderIds == null || memberOrderIds.length == 0) return "";
        
        // 获取最新的会员订单ID（最后一个）
        int memberOrderId = memberOrderIds[memberOrderIds.length - 1];
        if (memberOrderId > 0) {
            List<LitemallOrderGoods>  memberOrderGoods = this.orderGoodsService.queryByOid( memberOrderId);
            if (memberOrderGoods != null && memberOrderGoods.size() > 0) {
                LitemallOrderGoods orderGoods = memberOrderGoods.get(0);
                if (orderGoods!=null) {
                    LitemallGoods memberGoods = this.goodsService.findById(orderGoods.getGoodsId());
                    if (memberGoods != null) {
                        return memberGoods.getKeywords();
                    }
                }
            }
        }
        return "";
    }    

    /**
     * 获取会员的功能列表
     * @return
     */
    public List<MemberType> getMemberFeatures() {
        String fileName = "member-features.json";
        List<MemberType> memberTypes;
        try {
            memberTypes = DbUtil.readJsonFileAsObject(fileName, List.class, objectMapper);
        } catch (Exception e) {
            logger.error("读取会员功能列表失败。 " + e.getMessage());
            memberTypes = new ArrayList<>();
        }
        return memberTypes;
    }
}