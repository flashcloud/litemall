package org.linlinjava.litemall.admin.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.notify.NotifyService;
import org.linlinjava.litemall.core.notify.NotifyType;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.*;
import org.linlinjava.litemall.db.exception.MaxTwoMemberOrderException;
import org.linlinjava.litemall.db.exception.MemberOrderDataException;
import org.linlinjava.litemall.db.service.*;
import org.linlinjava.litemall.db.util.CouponUserConstant;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.linlinjava.litemall.admin.util.AdminResponseCode.*;

@Service

public class AdminOrderService {
    private final Log logger = LogFactory.getLog(AdminOrderService.class);

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private LitemallOrderGoodsService orderGoodsService;
    @Autowired
    private LitemallOrderService orderService;
    @Autowired
    private LitemallGoodsProductService productService;
    @Autowired
    private LitemallUserService userService;
    @Autowired
    private LitemallCommentService commentService;
    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private NotifyService notifyService;
    @Autowired
    private LogHelper logHelper;
    @Autowired
    private LitemallCouponUserService couponUserService;
    @Autowired
    private LitemallMemberService memberService;

    public Object list(String nickname, String consignee, String orderSn, LocalDateTime start, LocalDateTime end, List<Short> orderStatusArray,
                       Integer page, Integer limit, String sort, String order) {
        Map<String, Object> data = (Map)orderService.queryVoSelective(nickname, consignee, orderSn, start, end, orderStatusArray, page, limit, sort, order);
        return ResponseUtil.ok(data);
    }

    public Object detail(Integer id) {
        LitemallOrder order = orderService.findById(id);
        if (order == null) {
            return ResponseUtil.fail();
        }
        List<LitemallOrderGoods> orderGoods = orderGoodsService.queryByOid(id);
        UserVo user = userService.findUserVoById(order.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("orderGoods", orderGoods);
        data.put("user", user);

        return ResponseUtil.ok(data);
    }

    /**
     * 订单退款
     * <p>
     * 1. 检测当前订单是否能够退款;
     * 2. 微信退款操作;
     * 3. 设置订单退款确认状态；
     * 4. 订单商品库存回库。
     * <p>
     * TODO
     * 虽然接入了微信退款API，但是从安全角度考虑，建议开发者删除这里微信退款代码，采用以下两步走步骤：
     * 1. 管理员登录微信官方支付平台点击退款操作进行退款
     * 2. 管理员登录litemall管理后台点击退款操作进行订单状态修改和商品库存回库
     *
     * @param body 订单信息，{ orderId：xxx }
     * @return 订单退款操作结果
     */
    @Transactional
    public Object refund(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        String refundMoney = JacksonUtil.parseString(body, "refundMoney");
        if (orderId == null) {
            return ResponseUtil.badArgument();
        }
        if (StringUtils.isEmpty(refundMoney)) {
            return ResponseUtil.badArgument();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        if (order.getActualPrice().compareTo(new BigDecimal(refundMoney)) != 0) {
            return ResponseUtil.badArgumentValue();
        }

        // 如果订单不是退款状态，则不能退款
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_REFUND)) {
            return ResponseUtil.fail(ORDER_CONFIRM_NOT_ALLOWED, "订单不能确认收货");
        }

        // 微信退款
        WxPayRefundRequest wxPayRefundRequest = new WxPayRefundRequest();
        wxPayRefundRequest.setOutTradeNo(order.getOrderSn());
        wxPayRefundRequest.setOutRefundNo("refund_" + order.getOrderSn());
        // 元转成分
        Integer totalFee = order.getActualPrice().multiply(new BigDecimal(100)).intValue();
        wxPayRefundRequest.setTotalFee(totalFee);
        wxPayRefundRequest.setRefundFee(totalFee);

        WxPayRefundResult wxPayRefundResult;
        try {
            wxPayRefundResult = wxPayService.refund(wxPayRefundRequest);
        } catch (WxPayException e) {
            logger.error(e.getMessage(), e);
            return ResponseUtil.fail(ORDER_REFUND_FAILED, "订单退款失败");
        }
        if (!wxPayRefundResult.getReturnCode().equals("SUCCESS")) {
            logger.warn("refund fail: " + wxPayRefundResult.getReturnMsg());
            return ResponseUtil.fail(ORDER_REFUND_FAILED, "订单退款失败");
        }
        if (!wxPayRefundResult.getResultCode().equals("SUCCESS")) {
            logger.warn("refund fail: " + wxPayRefundResult.getReturnMsg());
            return ResponseUtil.fail(ORDER_REFUND_FAILED, "订单退款失败");
        }

        LocalDateTime now = LocalDateTime.now();
        // 设置订单取消状态
        order.setOrderStatus(OrderUtil.STATUS_REFUND_CONFIRM);
        order.setEndTime(now);
        // 记录订单退款相关信息
        order.setRefundAmount(order.getActualPrice());
        order.setRefundType("微信退款接口");
        order.setRefundContent(wxPayRefundResult.getRefundId());
        order.setRefundTime(now);
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            throw new RuntimeException("更新数据已失效");
        }

        // 商品货品数量增加
        List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(orderId);
        for (LitemallOrderGoods orderGoods : orderGoodsList) {
            Integer productId = orderGoods.getProductId();
            Short number = orderGoods.getNumber();
            if (productService.addStock(productId, number) == 0) {
                throw new RuntimeException("商品货品库存增加失败");
            }
        }

        // 返还优惠券
        List<LitemallCouponUser> couponUsers = couponUserService.findByOid(orderId);
        for (LitemallCouponUser couponUser: couponUsers) {
            // 优惠券状态设置为可使用
            couponUser.setStatus(CouponUserConstant.STATUS_USABLE);
            couponUser.setUpdateTime(LocalDateTime.now());
            couponUserService.update(couponUser);
        }

        //TODO 发送邮件和短信通知，这里采用异步发送
        // 退款成功通知用户, 例如“您申请的订单退款 [ 单号:{1} ] 已成功，请耐心等待到账。”
        // 注意订单号只发后6位
        notifyService.notifySmsTemplate(order.getMobile(), NotifyType.REFUND,
                new String[]{order.getOrderSn().substring(8, 14)});

        logHelper.logOrderSucceed("退款", "订单编号 " + order.getOrderSn());
        return ResponseUtil.ok();
    }

    /**
     * 发货
     * 1. 检测当前订单是否能够发货
     * 2. 设置订单发货状态
     *
     * @param body 订单信息，{ orderId：xxx, shipSn: xxx, shipChannel: xxx }
     * @return 订单操作结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    @Transactional
    public Object ship(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        String shipSn = JacksonUtil.parseString(body, "shipSn");
        String shipChannel = JacksonUtil.parseString(body, "shipChannel");
        Integer parentOrderId = JacksonUtil.parseInteger(body, "parentOrderId");
        if (orderId == null || shipSn == null || shipChannel == null) {
            return ResponseUtil.badArgument();
        }

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        //设置发货商品的序列号等参数
        List<Map<String, Object>> goodsAttValues = null;
        try {
            //利用ObjectMapper将JSon字符串body中的goodsList部分转换成Map结构
            goodsAttValues = parseOrderGoodsAttributeValue(body);
        } catch (Exception e) {
            return ResponseUtil.fail(ORDER_PARSE_GOODS_INPUT_DATA, "解析发货商品参数失败: " + e.toString());
        }
        if (goodsAttValues != null) {
            // 更新发货商品的序列号等参数
            for (Map<String, Object> item : goodsAttValues) {
                Integer detId = Integer.parseInt(item.get("detId").toString());
                String serial =  item.get("serial").toString();
                String boundserial =  item.get("boundSerial").toString();
                Short maxClientsCount =  Short.parseShort(item.get("maxClientsCount").toString());
                Short maxRegisterUsersCount =  Short.parseShort(item.get("maxRegisterUsersCount").toString());
                LitemallOrderGoods orderGoods = orderGoodsService.findById(detId);
                orderGoods.setSerial(serial);
                orderGoods.setBoundSerial(boundserial); // 绑定的序列号不为空，表示此条订单是对应序列号的软件产品的服务或它的升级
                orderGoods.setMaxClientsCount(maxClientsCount);
                orderGoods.setMaxRegisterUsersCount(maxRegisterUsersCount);
                if (serial != null && !serial.isEmpty()) {
                    //如果有序列号，表示订购的软件，然后将订单用户绑定到orderGoods的hasRegisterUserIds字段
                    if (orderGoods.getHasRegisterUserIds() == null || orderGoods.getHasRegisterUserIds().length == 0) {
                        Integer[] hasRegisterUserIds = new Integer[]{order.getUserId()};
                        orderGoods.setHasRegisterUserIds(hasRegisterUserIds);
                    } else {
                        List<Integer> hasRegisterUserIdsList = new ArrayList<>(Arrays.asList(orderGoods.getHasRegisterUserIds()));
                        if (!hasRegisterUserIdsList.contains(order.getUserId())) {
                            hasRegisterUserIdsList.add(order.getUserId());
                            orderGoods.setHasRegisterUserIds(hasRegisterUserIdsList.toArray(new Integer[0]));
                        }
                    }
                } else {
                    //如果没有序列号，则清除hasRegisterUserIds字段
                    orderGoods.setHasRegisterUserIds(new Integer[0]);
                }
                orderGoodsService.updateById(orderGoods);
            }
        }

        // 如果订单不是已付款状态，则不能发货
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_PAY)) {
            return ResponseUtil.fail(ORDER_CONFIRM_NOT_ALLOWED, "订单不能确认收货");
        }

        //更新父级订单ID和根订单ID
        if (parentOrderId != null) {
            if (parentOrderId > 0) {
                LitemallOrder parentOrder = orderService.findById(parentOrderId);
                if (parentOrder != null) {
                    order.setParentOrderId(parentOrderId);
                    if (parentOrder.getRootOrderId() == 0) {
                        order.setRootOrderId(parentOrderId);
                    } else {
                        order.setRootOrderId(parentOrder.getRootOrderId());
                    }
                } else {
                    return ResponseUtil.fail(ORDER_PARENT_ORDER_NOT_EXISTS, "指定的上级关联订单ID：" + parentOrderId + " 不存在，请确认后再重试");
                }
            }
            if (parentOrderId == 0) {
                order.setParentOrderId(0);
                order.setRootOrderId(0);
            }
        }

        order.setOrderStatus(OrderUtil.STATUS_SHIP);
        order.setShipSn(shipSn);
        order.setShipChannel(shipChannel);
        order.setShipTime(LocalDateTime.now());
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            return ResponseUtil.updatedDateExpired();
        }

        //TODO 发送邮件和短信通知，这里采用异步发送
        // 发货会发送通知短信给用户:          *
        // "您的订单已经发货，快递公司 {1}，快递单 {2} ，请注意查收"
        notifyService.notifySmsTemplate(order.getMobile(), NotifyType.SHIP, new String[]{shipChannel, shipSn});

        logHelper.logOrderSucceed("发货", "订单编号 " + order.getOrderSn());
        return ResponseUtil.ok();
    }

    /**
     * 删除订单
     * 1. 检测当前订单是否能够删除
     * 2. 删除订单
     *
     * @param body 订单信息，{ orderId：xxx }
     * @return 订单操作结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    public Object delete(String body) {
        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }

        // 如果订单不是关闭状态(已取消、系统取消、已退款、用户已确认、系统已确认)，则不能删除
        // Short status = order.getOrderStatus();
        // if (!status.equals(OrderUtil.STATUS_CANCEL) && !status.equals(OrderUtil.STATUS_AUTO_CANCEL) &&
        //         !status.equals(OrderUtil.STATUS_CONFIRM) &&!status.equals(OrderUtil.STATUS_AUTO_CONFIRM) &&
        //         !status.equals(OrderUtil.STATUS_REFUND_CONFIRM)) {
        //     return ResponseUtil.fail(ORDER_DELETE_FAILED, "订单不能删除");
        // }
        
        //只能删除取消状态的订单
        if (!order.getCanDelete()) {
            return ResponseUtil.fail(ORDER_DELETE_FAILED, "只能删除取消状态的订单");
        }

        // 删除订单
        orderService.deleteById(orderId);
        // 删除订单商品
        orderGoodsService.deleteByOrderId(orderId);
        logHelper.logOrderSucceed("删除", "订单编号 " + order.getOrderSn());
        return ResponseUtil.ok();
    }

    /**
     * 回复订单商品
     *
     * @param body 订单信息，{ orderId：xxx }
     * @return 订单操作结果
     * 成功则 { errno: 0, errmsg: '成功' }
     * 失败则 { errno: XXX, errmsg: XXX }
     */
    public Object reply(String body) {
        Integer commentId = JacksonUtil.parseInteger(body, "commentId");
        if (commentId == null || commentId == 0) {
            return ResponseUtil.badArgument();
        }
        // 目前只支持回复一次
        LitemallComment comment = commentService.findById(commentId);
        if(comment == null){
            return ResponseUtil.badArgument();
        }
        if (!StringUtils.isEmpty(comment.getAdminContent())) {
            return ResponseUtil.fail(ORDER_REPLY_EXIST, "订单商品已回复！");
        }
        String content = JacksonUtil.parseString(body, "content");
        if (StringUtils.isEmpty(content)) {
            return ResponseUtil.badArgument();
        }
        // 更新评价回复
        comment.setAdminContent(content);
        commentService.updateById(comment);

        return ResponseUtil.ok();
    }

    @Transactional
    public Object pay(String body) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        Integer orderId = JacksonUtil.parseInteger(body, "orderId");
        String newMoney = JacksonUtil.parseString(body, "newMoney");

        if (orderId == null || StringUtils.isEmpty(newMoney)) {
            return ResponseUtil.badArgument();
        }
        BigDecimal actualPrice = new BigDecimal(newMoney);

        LitemallOrder order = orderService.findById(orderId);
        if (order == null) {
            return ResponseUtil.badArgument();
        }
        if (!order.getOrderStatus().equals(OrderUtil.STATUS_CREATE)) {
            return ResponseUtil.fail(ORDER_PAY_FAILED, "当前订单状态不支持线下收款");
        }

        order.setPayTime(LocalDateTime.now());
        order.setActualPrice(actualPrice);
        order.setOrderStatus(OrderUtil.STATUS_PAY);
        if (orderService.updateWithOptimisticLocker(order) == 0) {
            return WxPayNotifyResponse.fail("更新数据已失效");
        }

        //如果是购买会员，则进入会员订单及用户的会员到期日逻辑
        try {
            memberService.updateUserMemberStatus(order);
        } catch (IllegalArgumentException | MemberOrderDataException | MaxTwoMemberOrderException e) {
            //回滚事务
            transactionManager.rollback(status);
            return ResponseUtil.fail(ORDER_CHECKOUT_MEMBER_FAIL, e.getMessage());
        }

        return ResponseUtil.ok();
    }

    private List<Map<String, Object>> parseOrderGoodsAttributeValue (String jsonStr) throws JsonParseException, JsonMappingException, IOException  {
        // 1. 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 2. 解析整个 JSON 为 Map
        Map<String, Object> rootMap = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        // 3. 提取 goodsInputList
        List<Map<String, Object>> goodsInputMapList = (List<Map<String, Object>>) rootMap.get("goodsInputList");

        return goodsInputMapList;
    }
}
