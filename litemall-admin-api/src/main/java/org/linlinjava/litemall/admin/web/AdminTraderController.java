package org.linlinjava.litemall.admin.web;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.util.ResponseCode;
import org.linlinjava.litemall.core.util.RegexUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.service.LitemallOrderService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin/trader")
@Validated
public class AdminTraderController {

    @Autowired
    private LitemallTraderService traderService;

    @Autowired
    private LitemallOrderService orderService;

    @RequiresPermissions("admin:trader:list")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "交易企业查询")
    @GetMapping("/list")
    public Object list(String name,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LitemallTrader> traderList = traderService.querySelective(name, page, limit, sort, order);

        //添加交易企业下面绑定的用户
        for (LitemallTrader trader : traderList) {
            List<LitemallUser> userList = traderService.usedTraderByUsers(trader.getId());
            if (userList != null && userList.size() > 0) {
                trader.setUserIds(traderService.usedTraderByUserIds(trader.getId()));
            } else {
                trader.setUserIds(new Integer[0]);
            }
        }
        return ResponseUtil.okList(traderList);
    }

    @RequiresPermissions("admin:trader:list")
    @GetMapping("/dropdownList")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "交易企业下拉列表")
    public Object dropdownList() {
        List<LitemallTrader> traderList = traderService.queryAll();

        List<Map<String, Object>> data = new ArrayList<>(traderList.size());
        for (LitemallTrader trader : traderList) {
            Map<String, Object> item = new HashMap<>(2);
            item.put("value", trader.getId());
            item.put("label", trader.getNickname());
            data.add(item);
        }

        return ResponseUtil.okList(data);
    }
    
    @RequiresPermissions("admin:role:create")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "新增交易企业")
    @PostMapping("/create")
    public Object create(@RequestBody LitemallTrader trader) {
        if (traderService.checkCompanyExist(trader.getCompanyName())) {
            return ResponseUtil.fail(ResponseCode.TRADER_NAME_EXIST, "企业名称已经存在");
        }

        if (traderService.checkTaxidExist(trader.getTaxid())) {
            return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "该税号已经存在");
        }

        if (StringUtils.isNotBlank(trader.getPhoneNum()) && !RegexUtil.isPhoneOrMobile(trader.getPhoneNum())) {
            return ResponseUtil.fail(ResponseCode.PHONE_ERR, "电话格式有误");
        }

        Object error = validate(trader);
        if (error != null) {
            return error;
        }

        traderService.add(trader);

        return ResponseUtil.ok(trader);
    }

    @RequiresPermissions("admin:role:update")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "编辑交易企业")
    @PostMapping("/update")
    public Object update(@RequestBody LitemallTrader trader) {
        if (StringUtils.isNotBlank(trader.getPhoneNum()) && !RegexUtil.isPhoneOrMobile(trader.getPhoneNum())) {
            return ResponseUtil.fail(ResponseCode.PHONE_ERR, "电话格式有误");
        }

        Object error = validate(trader);
        if (error != null) {
            return error;
        }

        if (traderService.checkCompanyExist(trader.getCompanyName(), trader.getId())) {
            return ResponseUtil.fail(ResponseCode.TRADER_NAME_EXIST, "企业名称已经存在");
        }
        
        if (traderService.checkTaxidExist(trader.getTaxid(), trader.getId())) {
            return ResponseUtil.fail(ResponseCode.TRADER_TAXID_EXIST, "该税号已经存在");
        }

        traderService.updateById(trader);
        return ResponseUtil.ok();
    }

    @RequiresPermissions("admin:role:delete")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "删除交易企业")
    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallTrader role) {
        Integer id = role.getId();
        if (id == null) {
            return ResponseUtil.badArgument();
        }

        if (traderService.isTraderUsed(id)) {
            List<LitemallUser> userList = traderService.usedTraderByUsers(id);
            if (userList != null && userList.size() > 0) {
                return ResponseUtil.fail(ResponseCode.TRADER_REFERED_BY_USERS, "这些用户正在使用当前交易企业，不能删除:" + traderService.usedTraderUsersString(userList));
            }
        }

        if (orderService.countByTrader(id) > 0) return ResponseUtil.fail(ResponseCode.TRADER_HAS_ORDERS, "当前交易企业有订单，不能删除");
        

        traderService.deleteById(id);
        return ResponseUtil.ok();
    }
    
    private Object validate(LitemallTrader trader) {
        if (!traderService.validate(trader)) return ResponseUtil.badArgument();
        return null;
	}
}
