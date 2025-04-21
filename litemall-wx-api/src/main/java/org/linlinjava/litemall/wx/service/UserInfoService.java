package org.linlinjava.litemall.wx.service;

import java.util.LinkedList;
import java.util.List;

import org.linlinjava.litemall.db.domain.LitemallTrader;
import org.linlinjava.litemall.db.domain.LitemallUser;
import org.linlinjava.litemall.db.service.LitemallTraderService;
import org.linlinjava.litemall.db.service.LitemallUserService;
import org.linlinjava.litemall.wx.dto.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class UserInfoService {
    @Autowired
    private LitemallUserService userService;

    @Autowired
    private LitemallTraderService traderService;

    public UserInfo getInfo(Integer userId) {
        LitemallUser user = userService.findById(userId);
        Assert.state(user != null, "用户不存在");
        UserInfo userInfo = new UserInfo();
        userInfo.setNickName(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatar());
        return userInfo;
    }

    /**
     * 获取用户的商户列表
     * @param userId 用户ID
     * @return 商户列表
     */
    public List<LitemallTrader> getTraders(Integer userId) {
        return traderService.getTraders(userId);
    }

    public LitemallTrader getTrader(Integer userId, Integer  traderId) {
        List<LitemallTrader> traderList = getTraders(userId);
        if (traderList == null || traderList.size() == 0) return null;

        for (LitemallTrader trader : traderList) {
            if (trader.getId().equals(traderId)) {
                return trader;
            }
        }
        return null;
    }
}
