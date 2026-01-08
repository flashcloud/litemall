package org.linlinjava.litemall.wx.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.storage.StorageService;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.LitemallOrderGoods;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.domain.TraderOrderGoodsVo;
import org.linlinjava.litemall.db.service.LitemallMemberService;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallStorageService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.db.service.LitemallUserService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.WxOrderService;
import org.linlinjava.litemall.wx.util.WxResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.linlinjava.litemall.wx.util.WxResponseCode.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wx/member")
@Validated
public class WxMemberController {
    private final Log logger = LogFactory.getLog(WxMemberController.class);

    @Autowired LitemallTraderService traderService;
    @Autowired LitemallUserService userService;
    @Autowired LitemallOrderService orderService;
    @Autowired private LitemallMemberService memberService;
    @Autowired private WxOrderService wxOrderService;
    
    @GetMapping("memberInfo")
    public Object memberInfo(@LoginUser Integer userId, @RequestParam String serial) {
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        TraderOrderGoodsVo result = memberService.getMemberInfo(user, serial, true);
        if (result == null) {
            return ResponseUtil.fail(WxResponseCode.TRADER_NOT_BOUND, "无权查看该信息");
        }
        return ResponseUtil.ok(result);
    }
    @GetMapping("miniMember")
    public Object getMiniMemberData(@LoginUser Integer userId, @RequestParam String serial) {
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        if(serial == null || serial.trim().length() ==0){
            return ResponseUtil.fail(WxResponseCode.AUTH_DOG_KEY_EMPTY, "软件Key不能为空");
        }

        TraderOrderGoodsVo memberOrderGoods =  memberService.getMemberInfo(user, serial, true);
        if (memberOrderGoods == null) {
            return ResponseUtil.fail(WxResponseCode.TRADER_NOT_BOUND, "无权查看该信息");
        }
        Map<String, String> result = new HashMap<>();
        //如果是普通会员，则下面的取值必须与@link LitemallMemberService#createNormalMemberOrderGoods(LitemallUser, String)方法一致
        result.put("traderId", memberOrderGoods.getTraderId().toString());
        result.put("keywords", memberOrderGoods.getKeywords());
        result.put("goodsName", memberOrderGoods.getGoodsName());
        result.put("softwareName", memberOrderGoods.getRootOrderGoodsName() + "(" + String.join(",", memberOrderGoods.getRootOrderGoodsSpecifications()) + ")");
        result.put("expDateTime", memberOrderGoods.getExpDateTime().toString());
        return ResponseUtil.ok(result);
    }

    
}
