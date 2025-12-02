package org.linlinjava.litemall.wx.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.db.service.LitemallUserService;
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
	private LitemallUserService userService;    

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
        traderList.forEach(trader -> {
            if (!traderService.checkCanEditTrader(userId, trader.getId())) {
                trader.setDirectorName(userService.encryptUserNameAndPhone(trader.getDirectorName()));
                trader.setPhoneNum(userService.encryptTel(trader.getPhoneNum()));
            }
            trader.setShareCode("");
        });
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

        trader.setDisableUpdate(!traderService.checkCanEditTrader(userId, id));
        trader.setShareCode("");
        return ResponseUtil.ok(trader);
	}

    /**
	 * 删除我的公司
	 *
	 * @param userId 用户ID
	 * @param id     我的公司ID
	 * @return 
	 */
	@PostMapping("share")
	public Object share(@LoginUser Integer userId, @RequestBody LitemallTrader trader) {
		if (userId == null) {
			return ResponseUtil.unlogin();
		}
		Integer traderId = trader.getId();
		if (traderId == null) {
			return ResponseUtil.badArgument();
		}


		String shareCode = traderService.share(userId, traderId);
		return ResponseUtil.ok(shareCode);
	}

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

        if (!traderService.checkCanEditTrader(userId, trader.getId())) {
            LitemallUser user = userService.findById(userId);
            if ( user == null) {
                return ResponseUtil.badArgumentValue();
            }

            boolean unregResult = traderService.unregisterUser(user, litemallTrader);
            if (unregResult)  {
                return ResponseUtil.ok();
            }else {
                return ResponseUtil.fail();
            }
        }        

        if (litemallTrader.getUserId() != userId) {
            return ResponseUtil.fail(ResponseCode.TRADER_DEL_BY_OTHERS, "只能由公司的负责人(" + userService.getUserFullName(litemallTrader.getUserId()) + ")删除");
        }

        // 如果该商户有用户在使用，不能删除
        if (traderService.isTraderUsedByOtherUsers(userId, id)) {
            List<LitemallUser> userList = traderService.usedTraderByOtherUsers(userId, id);
            if (userList != null && userList.size() > 0) {
                return ResponseUtil.fail(ResponseCode.TRADER_REFERED_BY_USERS, "当前公司绑定有其他用户:\n" + traderService.usedTraderUsersString(userList));
            }
        }

        if (orderService.countByTrader(id) > 0) return ResponseUtil.fail(ResponseCode.TRADER_HAS_ORDERS, "当前交易企业有订单，不能删除");

        // List<LitemallTrader> traderList = userInfoService.getTraders(userId);
        // if (traderList.size() <= 1) {
        //     return ResponseUtil.fail(ResponseCode.TRADER_DEL_LAST, "必须至少保留一个公司");
        // }

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

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.badArgumentValue();
        }

		if (trader.getId() == null || trader.getId().equals(0)) {
            if (StringUtils.isNotBlank(trader.getPhoneNum()) && !RegexUtil.isPhoneOrMobile(trader.getPhoneNum())) {
                return ResponseUtil.fail(ResponseCode.PHONE_ERR, "电话格式有误");
            }

            Object error = validate(trader);
            if (error != null) {
                return error;
            }

			trader.setId(null);

            LitemallTrader dbTrader = traderService.existsInDBTrader(trader);

            if (dbTrader != null) {
                String creatorName = "";
                LitemallUser creator = userService.findById(dbTrader.getUserId());
                if (creator != null) {
                    if (creator.getId() == userId) {
                        return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "公司已存在，请不要重复添加");
                    }
                    creatorName = userService.encryptUserNameAndPhone(creator);
                }
                //trader = traderService.registerUser(user, dbTrader);
            
                return ResponseUtil.fail(ResponseCode.TRADER_NAME_EXIST, "公司已存在，请通过" + creatorName + "分享二维码扫码加入");
            } else { 
                trader.setUserId(userId);
                trader.setCreatorId(userId);
			    traderService.add(userId, trader);
            }
		} else {
			LitemallTrader litemallTrader = userInfoService.getTrader(userId, trader.getId() );
			if (litemallTrader == null) {
				return ResponseUtil.badArgumentValue();
			}

            if (!traderService.checkCanEditTrader(userId, trader.getId())) {
                if (trader.getIsDefault() &&  !litemallTrader.getIsDefault()) {
                    traderService.updateDefaultTrader(userId, trader);
                }
                LitemallUser traderUser = userService.findById(litemallTrader.getUserId());
                String traderUserFullName = traderUser == null ? "系统管理员" : userService.encryptUserNameAndPhone(traderUser);
                return ResponseUtil.fail(ResponseCode.TRADER_UPDATE_REJECT, "仅允许 " + traderUserFullName + " 修改");
            }

            if (traderService.checkCompanyExist(litemallTrader.getCompanyName(), trader.getId())) {
                return ResponseUtil.fail(ResponseCode.TRADER_NAME_EXIST, "企业名称已经存在");
            }
            
            if (traderService.checkTaxidExist(litemallTrader.getTaxid(), trader.getId())) {
                return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "该税号已经存在");
            }

            if (StringUtils.isNotBlank(trader.getPhoneNum()) && !RegexUtil.isPhoneOrMobile(trader.getPhoneNum())) {
                return ResponseUtil.fail(ResponseCode.PHONE_ERR, "电话格式有误");
            }

            Object error = validate(trader);
            if (error != null) {
                return error;
            }

			traderService.updateById(userId, trader);
		}
		return ResponseUtil.ok(trader.getId());
	}

    @GetMapping("boundByShare")
	public Object boundByShare(@LoginUser Integer userId, @NotNull String shareCode) {
		if (userId == null) {
			return ResponseUtil.unlogin();
		}

        LitemallUser user = userService.findById(userId);
        if (user == null) {
            return ResponseUtil.badArgumentValue();
        }

        Object result = traderService.boundTraderByShare(user, shareCode);
        if (result instanceof LitemallTrader) {
            return ResponseUtil.ok(result);
        } else if (result instanceof Integer && ((Integer) result) == 1) {
            return ResponseUtil.fail(ResponseCode.TRADER_SHARE_CODE_NOT_EXIST, "二维码失效，请重新分享后再扫码");
        } else if (result instanceof Integer && ((Integer) result) == 2) {
            return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "公司已存在，无需绑定");
        } else {
            return ResponseUtil.fail();
        }
    }

    private Object validate(LitemallTrader trader) {
        if (!traderService.validate(trader)) return ResponseUtil.badArgument();
        return null;
	}
}