package org.linlinjava.litemall.admin.web;

import io.swagger.models.auth.In;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.admin.util.AdminResponseCode;
import org.linlinjava.litemall.admin.util.Permission;
import org.linlinjava.litemall.admin.util.PermissionUtil;
import org.linlinjava.litemall.admin.vo.PermVo;
import org.linlinjava.litemall.core.util.JacksonUtil;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.LitemallAdmin;
import org.linlinjava.litemall.db.domain.LitemallPermission;
import org.linlinjava.litemall.db.domain.LitemallRole;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.service.LitemallAdminService;
import org.linlinjava.litemall.db.service.LitemallPermissionService;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Security;
import java.util.*;

import static org.linlinjava.litemall.admin.util.AdminResponseCode.*;

@RestController
@RequestMapping("/admin/trader")
@Validated
public class AdminTraderController {

    @Autowired
    private LitemallTraderService traderService;
    @Autowired
    private LitemallPermissionService permissionService;
    @Autowired
    private LitemallAdminService adminService;

    @RequiresPermissions("admin:trader:list")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "交易企业查询")
    @GetMapping("/list")
    public Object list(String name,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        List<LitemallTrader> roleList = traderService.querySelective(name, page, limit, sort, order);
        return ResponseUtil.okList(roleList);
    }  
    
    @RequiresPermissions("admin:role:create")
    @RequiresPermissionsDesc(menu = {"用户管理", "交易企业管理"}, button = "新增交易企业")
    @PostMapping("/create")
    public Object create(@RequestBody LitemallTrader trader) {
        Object error = validate(trader);
        if (error != null) {
            return error;
        }

        if (traderService.checkCompanyExist(trader.getCompanyName())) {
            return ResponseUtil.fail(TRADER_NAME_EXIST, "交易企业名称已经存在");
        }

        if (traderService.checkTaxidExist(trader.getTaxid())) {
            return ResponseUtil.fail(TRADER_TAXID_EXIST, "该税号已经存在");
        }        

        traderService.add(trader);

        return ResponseUtil.ok(trader);
    }

    @RequiresPermissions("admin:role:update")
    @RequiresPermissionsDesc(menu = {"系统管理", "角色管理"}, button = "角色编辑")
    @PostMapping("/update")
    public Object update(@RequestBody LitemallTrader trader) {
        Object error = validate(trader);
        if (error != null) {
            return error;
        }

        traderService.updateById(trader);
        return ResponseUtil.ok();
    }

    @RequiresPermissions("admin:role:delete")
    @RequiresPermissionsDesc(menu = {"系统管理", "角色管理"}, button = "角色删除")
    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallTrader role) {
        Integer id = role.getId();
        if (id == null) {
            return ResponseUtil.badArgument();
        }

        // TODO： 检查当前商户是否还有用户，有用户则不能删除
        /*List<LitemallAdmin> adminList = adminService.all();
        for (LitemallAdmin admin : adminList) {
            Integer[] roleIds = admin.getRoleIds();
            for (Integer roleId : roleIds) {
                if (id.equals(roleId)) {
                    return ResponseUtil.fail(ROLE_USER_EXIST, "当前角色存在管理员，不能删除");
                }
            }
        }*/

        traderService.deleteById(id);
        return ResponseUtil.ok();
    }
    
    private Object validate(LitemallTrader trader) {
        String name = trader.getCompanyName();
        if (StringUtils.isEmpty(name)) {
            return ResponseUtil.badArgument();
        }

        String taxid = trader.getTaxid();
        if (StringUtils.isEmpty(taxid)) {
            return ResponseUtil.badArgument();
        }

        return null;
    }    
}
