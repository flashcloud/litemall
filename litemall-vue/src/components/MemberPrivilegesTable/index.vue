<template>
  <div class="member-privileges-container">
    <h3 v-if="title" class="table-title"> {{ title.replace('金软', '金软助手') }}</h3>
    
    <div class="privileges-table-wrapper">
      <table class="member-privileges-table">
        <thead>
          <tr>
            <th class="privilege-column">特权</th>
            <th 
              v-for="(tab, index) in memberTabs" 
              :key="tab.id || index"
              :class="getMemberColumnClass(index)"
            >
              <div :class="getMemberHeaderClass(index)">
                <div class="member-title-line1">{{ getTitle(tab.title) }}</div>
                
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(privilege, key) in privilegeList" :key="key">
            <td class="privilege-column">{{ privilege.name }}</td>
            <td 
              v-for="(tab, index) in memberTabs" 
              :key="(tab.id || index) + '-' + key"
              :class="getMemberColumnClass(index)"
            >
              <span v-if="key === 'storage' && getStorageValue(tab, key)">
                <span class="storage-size">{{ getStorageValue(tab, key) }}</span>
              </span>
              <span v-else>
                <span 
                  v-if="isPrivilegeEnabled(tab, key)"
                  class="check-icon"
                >
                  ✓
                </span>
                <span 
                  v-else
                  class="dash-icon"
                >
                  —
                </span>
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemberPrivilegesTable',
  
  props: {
    // 会员标签数据
    memberTabs: {
      type: Array,
      required: true,
      default: function() {
        return [];
      }
    },
    
    // 表格标题
    title: {
      type: String,
      default: ''
    },
    
    // 权益列表配置
    privilegeList: {
      type: Object,
      default: function() {
        return {
          udi: { name: '离线UDI扫码', icon: 'scan' },
          trade: { name: '查询客户/供货商信息', icon: 'friends-o' },
          product: { name: '查询产品信息', icon: 'goods-collect-o' },
          stock: { name: '查询库存信息', icon: 'records' }
        };
      }
    },
    
    // 主题配置
    theme: {
      type: String,
      default: 'default', // default, golden, red
      validator: function(value) {
        return ['default', 'golden', 'red'].indexOf(value) !== -1;
      }
    }
  },
  
  methods: {
    // 获取会员列的样式类
    getMemberColumnClass: function(index) {
      var baseClass = 'member-column';
      
      if (this.theme === 'golden') {
        if (index === 0) return baseClass + ' svip-column';
        if (index === 1) return baseClass + ' vip-column';
        return baseClass + ' normal-column';
      }
      
      // 根据会员等级或索引返回不同样式
      if (index === 0) return baseClass + ' premium-column';
      if (index === 1) return baseClass + ' standard-column';
      return baseClass + ' basic-column';
    },
    
    // 获取会员标题样式类
    getMemberHeaderClass: function(index) {
      if (this.theme === 'golden') {
        if (index === 0) return 'svip-header';
        if (index === 1) return 'vip-header';
        return 'normal-header';
      }
      
      return 'member-header';
    },
    
    // 检查权益是否启用
    isPrivilegeEnabled: function(tab, key) {
      return tab.funcPoint && 
             tab.funcPoint[key] && 
             tab.funcPoint[key].enable === true;
    },
    
    // 获取存储空间值
    getStorageValue: function(tab, key) {
      if (tab.funcPoint && 
          tab.funcPoint[key] && 
          tab.funcPoint[key].value) {
        return tab.funcPoint[key].value;
      }
      return null;
    },
    // 获取标题
    getTitle: function(title) {
      if (!title) return '';
      
      // 如果标题包含特定关键词，进行智能分割
      if (title.includes('金软')) {
        return title.replace('金软', '');
      } else {
        return title;
      }
    }
  }
};
</script>

<style scoped lang="scss">
.member-privileges-container {
  width: 100%;
  padding: 0 16px;
  margin-bottom: 20px;
  
  .table-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
    text-align: left;
  }
}

.privileges-table-wrapper {
  width: 100%;
  overflow-x: auto;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  background-color: #fff;
}

.member-privileges-table {
  width: 100%;
  min-width: 400px;
  border-collapse: collapse;
  background-color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  
  th, td {
    padding: 14px 12px;
    text-align: center;
    border: none;
    font-size: 13px;
    vertical-align: middle;
  }
  
  th {
    font-weight: 600;
    color: #333;
    border-bottom: 1px solid #eee;
    position: sticky;
    top: 0;
    z-index: 10;
  }
  
  td {
    border-bottom: 1px solid #f5f5f5;
  }
  
  tr:last-child td {
    border-bottom: none;
  }
}

// 特权列样式
.privilege-column {
  background-color: #fafafa !important;
  color: #666;
  text-align: left !important;
  padding-left: 16px !important;
  min-width: 120px;
  position: sticky;
  left: 0;
  z-index: 5;
  border-right: 1px solid #eee;
}

// 会员列基础样式
.member-column {
  min-width: 100px;
  font-weight: 500;
}

// 黄金主题样式
.svip-column {
//   background: linear-gradient(135deg, #f5e6a3 0%, #f7e7a4 100%) !important;
  
  .svip-header {
    font-size: 16px;
    font-weight: bold;
    font-style: italic;
    line-height: 1.2;
    
    .member-title-line1,
    .member-title-line2 {
      font-size: 16px;
    }
  }
}

.vip-column {
  background-color: #fff5f5 !important;
  color: #e74c3c;
  
  .vip-header {
    font-size: 16px;
    font-weight: bold;
    font-style: italic;
    color: #e74c3c;
    line-height: 1.2;
    
    .member-title-line1,
    .member-title-line2 {
      font-size: 16px;
    }
  }
}

.normal-column {
  background-color: #f8f9fa !important;
  color: #666;
  
  .normal-header {
    font-size: 14px;
    font-weight: 600;
    color: #666;
    line-height: 1.2;
    
    .member-title-line1,
    .member-title-line2 {
      font-size: 14px;
    }
  }
}

// 默认主题样式
.premium-column {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: white;
}

.standard-column {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%) !important;
  color: white;
}

.basic-column {
  background-color: #f1f3f4 !important;
  color: #666;
}

.member-header {
  font-size: 14px;
  font-weight: bold;
  line-height: 1.2;
  
  .member-title-line1,
  .member-title-line2 {
    font-size: 14px;
  }
}

// 图标样式
.check-icon {
  color: #27ae60;
  font-size: 18px;
  font-weight: bold;
  display: inline-block;
  animation: checkPulse 2s infinite ease-in-out;
}

.dash-icon {
  color: #95a5a6;
  font-size: 18px;
  font-weight: bold;
}

// 存储空间特殊样式
.storage-size {
  font-size: 16px;
  font-weight: bold;
  color: inherit;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
}

// 动画效果
@keyframes checkPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .member-privileges-table {
    font-size: 12px;
    
    th, td {
      padding: 10px 8px;
    }
    
    .privilege-column {
      padding-left: 12px !important;
      min-width: 100px;
    }
    
    .svip-header,
    .vip-header,
    .member-header {
      font-size: 12px;
      
      .member-title-line1,
      .member-title-line2 {
        font-size: 12px;
      }
    }
    
    .storage-size {
      font-size: 14px;
      padding: 2px 6px;
    }
  }
}

@media (max-width: 480px) {
  .member-privileges-container {
    padding: 0 8px;
  }
  
  .member-privileges-table {
    font-size: 11px;
    
    th, td {
      padding: 8px 6px;
    }
    
    .privilege-column {
      min-width: 80px;
    }
    
    .svip-header,
    .vip-header,
    .member-header {
      .member-title-line1,
      .member-title-line2 {
        font-size: 14px;
      }
    }
  }
}
</style>