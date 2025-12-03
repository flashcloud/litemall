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

@RestController
@RequestMapping("/wx/member")
@Validated
public class WxMemberController {
    private final Log logger = LogFactory.getLog(WxMemberController.class);

    @Autowired LitemallTraderService traderService;
    @Autowired LitemallUserService userService;
    @Autowired LitemallOrderService orderService;
    @Autowired private LitemallMemberService memberService;    
    
    @GetMapping("memberInfo")
    public Object memberInfo(@LoginUser Integer userId, @RequestParam String serial) {
        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.fail(AUTH_INVALID_ACCOUNT, "用户不存在");
        }

        TraderOrderGoodsVo result = null;
        TraderOrderGoodsVo normalMember = new TraderOrderGoodsVo();
        normalMember.setId(0);
        normalMember.setGoodsName("普通卡");

        Integer[] memberOrderIds = user.getMemberOrderIds();
        if (memberOrderIds == null || memberOrderIds.length == 0) {
            result = normalMember;
        } else {
            result =  memberService.findMemberOrder(user, serial);
            if (result == null){
                result = normalMember;
            } else if (result.getId() == null || result.getId() == 0) {
                return ResponseUtil.fail(WxResponseCode.TRADER_NOT_BOUND, "无权查看该信息");
            }
        }

        return ResponseUtil.ok(result);
    }
}
