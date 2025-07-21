package org.linlinjava.litemall.db.dao;

import org.apache.ibatis.annotations.Param;
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
    List<TraderOrderGoodsVo> getTraderOrderedPCAppByUserId(@Param("userId") Integer userId);
}