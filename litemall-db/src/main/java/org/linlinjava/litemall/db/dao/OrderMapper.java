package org.linlinjava.litemall.db.dao;

import org.apache.ibatis.annotations.Param;
import org.linlinjava.litemall.db.domain.LitemallAddressExample;
import org.linlinjava.litemall.db.domain.LitemallAftersale;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.OrderVo;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderMapper {
    int updateWithOptimisticLocker(@Param("lastUpdateTime") LocalDateTime lastUpdateTime, @Param("order") LitemallOrder order);
    List<Map> getOrderIds(@Param("query") String query, @Param("orderByClause") String orderByClause);
    List<OrderVo> getOrderList(@Param("query") String query, @Param("orderByClause") String orderByClause);
    TraderOrderGoodsVo getTraderOrderedPCAppBy(@Param("serial") String serial, @Param("userId") Integer userId, @Param("id") Integer id);
    TraderOrderGoodsVo getTraderOrderedPCAppById(@Param("userId") Integer userId, @Param("id") Integer id);
    TraderOrderGoodsVo getTraderOrderedPCAppBySerial(@Param("serial") String serial);
    long getTraderOrderedGoodsCountByUserId(@Param("userId") Integer userId);
    List<TraderOrderGoodsVo> getTraderOrderedGoodsByUserId(@Param("userId") Integer userId);
    List<TraderOrderGoodsVo> getTraderOrderedPCAppByOther(@Param("userId") Integer userId, @Param("traderId") Integer traderId);
    // 商户订购指定KEY的PC软件。注意，这里虽然返回的是TraderOrderGoodsVo，但实际上返回的不是会员订单，而是PC软件订单。并且orderId为PC软件订单id，id为PC软件订单明细的id，而不是会员订单的id
    TraderOrderGoodsVo ordredPCApp(@Param("traderId") Integer traderId, @Param("serial") String serial);
    List<TraderOrderGoodsVo> getUserNoCheckedButPayedMember(@Param("userId") Integer userId, @Param("pcAppOrderId") Integer pcAppOrderId);
    TraderOrderGoodsVo getMemberOrderByOrderId(@Param("userId") Integer userId, @Param("memberOrderId") Integer memberOrderId, @Param("serial") String serial);

    // 插入试用会员订单及订单商品记录
    int insertTrialMember(@Param("userId") Integer userId, @Param("mobile") String mobile, @Param("orderSn") String orderSn);
    // 插入会员订单及订单商品记录。serial为软件Key
    int insertTrialMemberGoods(@Param("orderId") Integer orderId, @Param("serial") String serial, @Param("expDate") LocalDateTime expDate);
}