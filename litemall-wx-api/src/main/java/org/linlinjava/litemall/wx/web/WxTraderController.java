package org.linlinjava.litemall.wx.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.wx.annotation.LoginUser;
import org.linlinjava.litemall.wx.service.UserInfoService;
import org.linlinjava.litemall.core.util.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户收货地址服务
 */
@RestController
@RequestMapping("/wx/trader")
@Validated
public class WxTraderController {
	private final Log logger = LogFactory.getLog(WxTraderController.class);

	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private LitemallTraderService traderService;

    @Autowired
    private LitemallOrderService orderService;

	/**
	 * 用户所属的交易商户列表
	 *
	 * @param userId 用户ID
	 * @return 交易商户列表
	 */
	@GetMapping("list")
	public Object list(@LoginUser Integer userId) {
		if (userId == null) {
			return ResponseUtil.unlogin();
		}
		List<LitemallTrader> traderList = userInfoService.getTraders(userId);
		return ResponseUtil.okList(traderList);
	}

    /**
	 * 我的公司详情
	 *
	 * @param userId 用户ID
	 * @param id     收货地址ID
	 * @return 交易商户详情
	 */
	@GetMapping("detail")
	public Object detail(@LoginUser Integer userId, @NotNull Integer id) {
		if (userId == null) {
			return ResponseUtil.unlogin();
		}

        LitemallTrader trader = userInfoService.getTrader(userId, id);
        if (trader == null) {
            return ResponseUtil.badArgumentValue();
        }
        return ResponseUtil.ok(trader);
	}

    /**
	 * 删除我的公司
	 *
	 * @param userId 用户ID
	 * @param id     我的公司ID
	 * @return 
	 */
	@PostMapping("delete")
	public Object delete(@LoginUser Integer userId, @RequestBody LitemallTrader trader) {
		if (userId == null) {
			return ResponseUtil.unlogin();
		}
		Integer id = trader.getId();
		if (id == null) {
			return ResponseUtil.badArgument();
		}
		LitemallTrader litemallTrader = traderService.queryById(id);
		if (litemallTrader == null) {
			return ResponseUtil.badArgumentValue();
		}

        // 如果该商户有用户在使用，不能删除
        if (traderService.isTraderUsedByOtherUsers(userId, id)) {
            List<LitemallUser> userList = traderService.usedTraderByOtherUsers(userId, id);
            if (userList != null && userList.size() > 0) {
                return ResponseUtil.fail(ResponseCode.TRADER_REFERED_BY_USERS, "当前公司绑定有其他用户:\n" + traderService.usedTraderUsersString(userList));
            }
        }

        if (orderService.countByTrader(id) > 0) return ResponseUtil.fail(ResponseCode.TRADER_HAS_ORDERS, "当前交易企业有订单，不能删除");

		traderService.deleteByUser(userId, id);
		return ResponseUtil.ok();
	}

    /**
	 * 添加或更新我的公司
	 *
	 * @param userId  用户ID
	 * @param trader 用户公司
	 * @return 添加或更新操作结果
	 */
	@PostMapping("save")
	public Object save(@LoginUser Integer userId, @RequestBody LitemallTrader trader) {
		if (userId == null) {
			return ResponseUtil.unlogin();
		}

        if (StringUtils.isNotBlank(trader.getPhoneNum()) && !RegexUtil.isPhoneOrMobile(trader.getPhoneNum())) {
            return ResponseUtil.fail(ResponseCode.PHONE_ERR, "电话格式有误");
        }

		if (trader.getId() == null || trader.getId().equals(0)) {
            // TODO: 交易商户的默认选项
			// if (trader.getIsDefault()) {
			// 	// 重置其他收货地址的默认选项
			// 	traderService.resetDefault(userId);
			// }

            if (traderService.checkCompanyExist(trader.getCompanyName())) {
                return ResponseUtil.fail(ResponseCode.TRADER_NAME_EXIST, "企业名称已经存在");
            }
            
            if (traderService.checkTaxidExist(trader.getTaxid())) {
                return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "该税号已经存在");
            }

            Object error = validate(trader);
            if (error != null) {
                return error;
            }            

			trader.setId(null);
			trader.setUserId(userId);
			traderService.add(trader);
		} else {
			LitemallTrader litemallTrader = userInfoService.getTrader(userId, trader.getId() );
			if (litemallTrader == null) {
				return ResponseUtil.badArgumentValue();
			}

            if (traderService.checkCompanyExist(trader.getCompanyName(), trader.getId())) {
                return ResponseUtil.fail(ResponseCode.TRADER_NAME_EXIST, "企业名称已经存在");
            }
            
            if (traderService.checkTaxidExist(trader.getTaxid(), trader.getId())) {
                return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "该税号已经存在");
            }

            Object error = validate(trader);
            if (error != null) {
                return error;
            }            

            // TODO: 交易商户的默认选项
			// if (trader.getIsDefault()) {
			// 	// 重置其他收货地址的默认选项
			// 	addressService.resetDefault(userId);
			// }

			trader.setUserId(userId);
			traderService.updateById(trader);
		}
		return ResponseUtil.ok(trader.getId());
	}

    private Object validate(LitemallTrader trader) {
        if (!traderService.validate(trader)) return ResponseUtil.badArgument();
        return null;
	}
}