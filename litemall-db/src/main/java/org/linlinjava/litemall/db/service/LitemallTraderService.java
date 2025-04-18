package org.linlinjava.litemall.db.service;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import org.linlinjava.litemall.db.dao.LitemallTraderMapper;
import org.linlinjava.litemall.db.domain.LitemallTraderExample;
import org.springframework.stereotype.Service;
import org.linlinjava.litemall.db.domain.LitemallRole;
import org.linlinjava.litemall.db.domain.LitemallRoleExample;
import org.linlinjava.litemall.db.domain.LitemallTrader;
import com.alibaba.druid.util.StringUtils;
import com.github.pagehelper.PageHelper;

@Service
public class LitemallTraderService {
    @Resource
    private LitemallTraderMapper traderMapper;
    public List<LitemallTrader> querySelective(String name, Integer page, Integer limit, String sort, String order) {
        LitemallTraderExample example = new LitemallTraderExample();
        LitemallTraderExample.Criteria criteria = example.createCriteria();

        if (!StringUtils.isEmpty(name)) {
            criteria.andCompanyNameLike("%" + name + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return traderMapper.selectByExample(example);
    }

    public void add(LitemallTrader trader) {
        trader.setAddTime(LocalDateTime.now());
        trader.setUpdateTime(LocalDateTime.now());
        traderMapper.insertSelective(trader);
    }

    public void updateById(LitemallTrader trader) {
        trader.setUpdateTime(LocalDateTime.now());
        traderMapper.updateByPrimaryKeySelective(trader);
    }

    public void deleteById(Integer id) {
        traderMapper.logicalDeleteByPrimaryKey(id);
    }

    public List<LitemallTrader> queryAll() {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andDeletedEqualTo(false);
        return traderMapper.selectByExample(example);
    }

    public boolean checkCompanyExist(String name) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andCompanyNameEqualTo(name).andDeletedEqualTo(false);
        return traderMapper.countByExample(example) != 0;
    }

    /**
     * 检查税号是否存在
     * @param taxid
     * @return
     */
    public boolean checkTaxidExist(String taxid) {
        LitemallTraderExample example = new LitemallTraderExample();
        example.or().andTaxidEqualTo(taxid).andDeletedEqualTo(false);
        return traderMapper.countByExample(example) != 0;
    }    
}
