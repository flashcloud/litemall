package org.linlinjava.litemall.core.notify;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.domain.LitemallGoodsSpecification;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.exception.MemberOrderDataException;
import org.linlinjava.litemall.db.service.LitemallAdminService;
import org.linlinjava.litemall.db.service.LitemallGoodsService;
import org.linlinjava.litemall.db.service.LitemallMemberService;
import org.linlinjava.litemall.db.service.LitemallUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;

@Service
public class CommonNotifyService {
    private final Log logger = LogFactory.getLog(CommonNotifyService.class);

    @Autowired
    private NotifyService notifyService;
    @Autowired
    private LitemallAdminService adminService;
    @Autowired
    private LitemallGoodsService goodsService;
    @Autowired
    private LitemallMemberService memberService;
    @Autowired
    private LitemallUserService userService;

    /**
     * 订单支付成功以后，会发送短信给用户，以及发送邮件给管理员
     * @param isMemberOrder
     * @param order
     * @param user
     */
    public void notifyWhenPaySucceed(LitemallOrder order, boolean isSendAdmins) {
        LitemallUser user = userService.findById(order.getUserId());
        if (user == null) return;

        boolean isMemberOrder = memberService.isMemberOrder(order.getId());

        // 订单支付成功以后，会发送短信给用户，以及发送邮件给管理员
        notifyService.notifyMail("新订单通知", order.toString());
        // 这里微信的短信平台对参数长度有限制，所以将订单号只截取后6位
        final String orderSnOfPart = order.getOrderSn().substring(8, 14);
        boolean sendMemberSms = true;
        NotifyType notifyType = null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", "1234"); //Code参数，必传
        params.put("nickName", user.getNickname());
        if(isMemberOrder){
            notifyType = NotifyType.ORDER_MEMBER;
            params.put("now", order.getPayTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            try {
                LitemallGoodsSpecification memberSpeci = memberService.queryMemberGoodsSpecification(order);
                LitemallGoods memberGoods = goodsService.findById(memberSpeci.getGoodsId());
                params.put("memberType", memberGoods.getName() + " - " + memberSpeci.getValue());
                if (memberSpeci.getValue().contains("连续")) {
                    params.put("continueType", "自动续费");
                } else {
                    params.put("continueType", "不再享受会员权益");
                }
            } catch (IllegalArgumentException | MemberOrderDataException e) {
                logger.error("获取会员订单信息失败，无法发送会员购买短信，订单ID：" + order.getId());
                sendMemberSms = false;
            }
            params.put("orderSn", order.getOrderSn());
        } else {
            notifyType = NotifyType.PAY_SUCCEED;
            params.put("orderSn", order.getOrderSn());
            String orderPrice = order.getActualPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            params.put("totalAmount", orderPrice);
        }

        if (isSendAdmins && sendMemberSms && order.getMobile() != null && RegexUtil.isMobileSimple(order.getMobile())) {
            notifyService.notifySmsTemplateSync(order.getMobile(), notifyType, params);

            // 通知商户
            List<String> adminMobiles = adminService.allGetedSmsPhones();
            for (String adminMobile : adminMobiles) {
                notifyService.notifySmsTemplateSync(adminMobile, notifyType, params);
            }
        }
    }
}
