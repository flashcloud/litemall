package org.linlinjava.litemall.wx.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.storage.StorageService;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.BankAccountInfo;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.MemberType;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;
import org.linlinjava.litemall.db.service.LitemallStorageService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.db.service.LitemallUserService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.dto.UserInfo;
import org.linlinjava.litemall.wx.service.WxOrderService;
import org.linlinjava.litemall.wx.util.WxResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import static org.linlinjava.litemall.wx.util.WxResponseCode.*;

@RestController
@RequestMapping("/wx/order")
@Validated
public class WxOrderController {
    private final Log logger = LogFactory.getLog(WxOrderController.class);

    @Autowired
    private WxOrderService wxOrderService;

    @Autowired LitemallTraderService traderService;
    @Autowired LitemallUserService userService;
    @Autowired LitemallStorageService litemallStorageService;
    @Autowired private StorageService storageService;
    

    /**
     * 订单列表
     *
     * @param userId   用户ID
     * @param showType 显示类型，如果是0则是全部订单
     * @param page     分页页数
     * @param limit     分页大小
     * @param sort     排序字段
     * @param order     排序方式
     * @return 订单列表
     */
    @GetMapping("list")
    public Object list(@LoginUser Integer userId,
                       @RequestParam(defaultValue = "0") Integer showType,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        return wxOrderService.list(userId, showType, page, limit, sort, order);
    }

    /**
     * 订单详情
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    @GetMapping("detail")
    public Object detail(@LoginUser Integer userId, @NotNull Integer orderId) {
        return wxOrderService.detail(userId, orderId);
    }
    @GetMapping("traderOrderDetail")
    public Object traderOrderDetail(@LoginUser Integer userId, @RequestParam String serial, HttpServletRequest request) {
        LitemallUser user = null;
        //如果request是从本地访问，则允许查看
        String remoteAddr = request.getRemoteAddr();
        if (!remoteAddr.equals("127.0.0.1")) {
            user = userService.findById(userId);
            if (user == null) {
                return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
            }
        }
        TraderOrderGoodsVo result =  wxOrderService.getTraderOrderedPCAppBySerial( user != null && user.getTraderIds().length == 0 ? null : user, serial);//刚注册的用户，没有绑定到任何商户，所以不能传入user，不然查不到订单。//TODO:serial
        if (result == null){
            return ResponseUtil.fail(WxResponseCode.AUTH_DOG_KEY_NOT_EXISTS, "KEY号" + serial + "对应的记录不存在");
        } else if (result.getId() == null || result.getId() == 0) {
            return ResponseUtil.fail(WxResponseCode.TRADER_NOT_BOUND, "无权查看该信息");
        }
        //result.setPrice(BigDecimal.valueOf(0.0));//价格不显示
        return ResponseUtil.ok(result);
    }


    /**
     * 交易商户的订单产品条数
     *
     * @param userId 用户ID
     * @return 交易商户订单产品条数
     */
    @GetMapping("traderOrdersGoodsCount")
    public Object traderOrdersGoodsCount(@LoginUser Integer userId) {
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        long result =  wxOrderService.countTraderOrderedGoodsByUser(user);
        return ResponseUtil.ok(result);
    }

    /**
     * 交易商户订单列表
     *
     * @param userId 用户ID
     * @return 交易商户订单列表
     */
    @GetMapping("traderOrders")
    public Object traderOrdersData(@LoginUser Integer userId) {
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        List<TraderOrderGoodsVo> result =  wxOrderService.getTraderOrderedGoodsByUser(user);
        if (result == null || result.isEmpty()) {
            return ResponseUtil.okList(result);
        }
        return ResponseUtil.okList(result);
    }

/**
     * 交易商户订购的软件订单列表
     *
     * @param userId 用户ID
     * @return 交易商户订单列表
     */
    @GetMapping("traderOrderPcAppList")
    public Object traderOrderPcAppList(@LoginUser Integer userId) {
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        List<TraderOrderGoodsVo> result =  wxOrderService.getTraderOrderedPCAppByUser(user);
        if (result == null || result.isEmpty()) {
            return ResponseUtil.okList(result);
        }
        return ResponseUtil.okList(result);
    }

    /**
     * 根据订单明细ID获取指定管理用户的商户订阅的PC端应用已经注册的用户列表
     * @param userId 管理用户ID
     * @param orderGoodsId 订单ID
     * @return 用户列表
     */
    @GetMapping("pcAppRegisteredUsers")
    public Object traderOrderedPCAppRegisteredUsers(@LoginUser Integer userId, @NotNull Integer orderGoodsId) {
        List<UserInfo> userInfoList = new ArrayList<>();
        List<LitemallUser> users = wxOrderService.getTraderOrderedPCAppRegisteredUsers(userId, orderGoodsId);
        if (users == null || users.isEmpty()) {
            return ResponseUtil.okList(userInfoList);
        }

        for (LitemallUser user : users) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserName(user.getUsername());
            userInfo.setNickName(user.getNickname());
            userInfo.setAvatarUrl(user.getAvatar());
            userInfo.setGender(user.getGender());
            userInfo.setMobile(user.getMobile());
            userInfo.setAddTime(user.getAddTime());
            userInfoList.add(userInfo);
        }

        return ResponseUtil.okList(userInfoList);
    }

    @GetMapping("checkMemberStatus")
    public Object checkMemberStatus(@LoginUser Integer userId, @RequestParam String serial) { 
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }
        return wxOrderService.checkMemberStatus(user, serial);
    }
    
    /**
     * 提交订单
     *
     * @param userId 用户ID
     * @param body   订单信息，{ cartId：xxx, addressId: xxx, couponId: xxx, message: xxx, grouponRulesId: xxx,  grouponLinkId: xxx}
     * @return 提交订单操作结果
     */
    @PostMapping("submit")
    public Object submit(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        Object result = wxOrderService.submit(userId, body, request);
        if (result != null) {
            //如果提交失败，则删除可能上传的图片
            if (!ResponseUtil.isOk(result) && JacksonUtil.hasField(body, "payVoucher") && JacksonUtil.hasField(body, "fileKey")) {
                String fileKey = JacksonUtil.parseString(body, "fileKey");
                if (fileKey != null && !fileKey.isEmpty()) {
                    litemallStorageService.physicalDeleteByKey(fileKey);
                    storageService.delete(fileKey);
                }
            }
        }
        return result;
    }

    /**
     * 取消订单
     *
     * @param userId 用户ID
     * @param body   订单信息，{ orderId：xxx }
     * @return 取消订单操作结果
     */
    @PostMapping("cancel")
    public Object cancel(@LoginUser Integer userId, @RequestBody String body) {
        return wxOrderService.cancel(userId, body);
    }

    /**
     * 付款订单的预支付会话标识
     *
     * @param userId 用户ID
     * @param body   订单信息，{ orderId：xxx }
     * @return 支付订单ID
     */
    @PostMapping("prepay")
    public Object prepay(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        return wxOrderService.prepay(userId, body, request);
    }

    /**
     * 微信H5支付
     * @param userId
     * @param body
     * @param request
     * @return
     */
    @PostMapping("h5pay")
    public Object h5pay(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
        return wxOrderService.h5pay(userId, body, request);
    }

    /**
     * 微信付款成功或失败回调接口
     * <p>
     *  TODO
     *  注意，这里pay-notify是示例地址，建议开发者应该设立一个隐蔽的回调地址
     *
     * @param request 请求内容
     * @param response 响应内容
     * @return 操作结果
     */
    @PostMapping("pay-notify")
    public Object payNotify(HttpServletRequest request, HttpServletResponse response) {
        return wxOrderService.payNotify(request, response);
    }

    /**
     * 订单申请退款
     *
     * @param userId 用户ID
     * @param body   订单信息，{ orderId：xxx }
     * @return 订单退款操作结果
     */
    @PostMapping("refund")
    public Object refund(@LoginUser Integer userId, @RequestBody String body) {
        return wxOrderService.refund(userId, body);
    }

    /**
     * 确认收货
     *
     * @param userId 用户ID
     * @param body   订单信息，{ orderId：xxx }
     * @return 订单操作结果
     */
    @PostMapping("confirm")
    public Object confirm(@LoginUser Integer userId, @RequestBody String body) {
        return wxOrderService.confirm(userId, body);
    }

    /**
     * 删除订单
     *
     * @param userId 用户ID
     * @param body   订单信息，{ orderId：xxx }
     * @return 订单操作结果
     */
    @PostMapping("delete")
    public Object delete(@LoginUser Integer userId, @RequestBody String body) {
        return wxOrderService.delete(userId, body);
    }

    /**
     * 待评价订单商品信息
     *
     * @param userId  用户ID
     * @param ogid 订单商品ID
     * @return 待评价订单商品信息
     */
    @GetMapping("goods")
    public Object goods(@LoginUser Integer userId,
                        @NotNull Integer ogid) {
        return wxOrderService.goods(userId, ogid);
    }

    /**
     * 评价订单商品
     *
     * @param userId 用户ID
     * @param body   订单信息，{ orderId：xxx }
     * @return 订单操作结果
     */
    @PostMapping("comment")
    public Object comment(@LoginUser Integer userId, @RequestBody String body) {
        return wxOrderService.comment(userId, body);
    }

    @GetMapping("/shopBankAccounts")
    public Object getShopBankAccount() {
        List<BankAccountInfo> bankAccounts = wxOrderService.getShopBankAccounts();
        return ResponseUtil.ok(bankAccounts);
    }

}