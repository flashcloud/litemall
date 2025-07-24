<template>
  <div class="custom-tabs">
    <div class="tabs-header">
      <div
        v-for="(tab, index) in tabs"
        :key="index"
        :class="['tab-item', { active: currentTab === index }]"
        @click="handleTabClick(index)"
      >
        <div class="tab-content">
          <div class="tab-title">{{ tab.title }}</div>
          <div v-if="tab.subtitle" class="tab-subtitle">{{ tab.subtitle }}</div>
          <div v-if="tab.badge" class="tab-badge">{{ tab.badge }}</div>
        </div>
      </div>
    </div>
    
    <!-- 选项卡内容区域 -->
    <div class="tabs-content">
      <slot :currentTab="currentTab" :activeTabData="tabs[currentTab]"></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CustomTabs',
  props: {
    tabs: {
      type: Array,
      required: true,
      default: () => []
    },
    value: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      currentTab: this.value
    };
  },
  watch: {
    value(newVal) {
      this.currentTab = newVal;
    },
    currentTab(newVal) {
      this.$emit('input', newVal);
      this.$emit('change', newVal, this.tabs[newVal]);
    }
  },
  methods: {
    handleTabClick(index) {
      if (index !== this.currentTab) {
        this.currentTab = index;
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.custom-tabs {
  background-color: #fff;
  
  .tabs-header {
    display: flex;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px 12px 0 0;
    padding: 4px;
    position: relative;
    
    .tab-item {
      flex: 1;
      padding: 12px 8px;
      text-align: center;
      cursor: pointer;
      border-radius: 8px;
      transition: all 0.3s ease;
      position: relative;
      
      .tab-content {
        position: relative;
        z-index: 2;
      }
      
      .tab-title {
        font-size: 16px;
        font-weight: 500;
        color: rgba(255, 255, 255, 0.7);
        margin-bottom: 2px;
        transition: color 0.3s ease;
      }
      
      .tab-subtitle {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.5);
        line-height: 1.2;
      }
      
      .tab-badge {
        position: absolute;
        top: -4px;
        right: 8px;
        background: #ff4757;
        color: white;
        font-size: 10px;
        padding: 2px 6px;
        border-radius: 10px;
        min-width: 16px;
        text-align: center;
        line-height: 1.2;
        font-weight: 500;
      }
      
      &.active {
        background: rgba(255, 255, 255, 0.2);
        backdrop-filter: blur(10px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        
        .tab-title {
          color: #fff;
          font-weight: 600;
        }
        
        .tab-subtitle {
          color: rgba(255, 255, 255, 0.8);
        }
      }
      
      &:hover:not(.active) {
        background: rgba(255, 255, 255, 0.1);
        
        .tab-title {
          color: rgba(255, 255, 255, 0.9);
        }
      }
    }
  }
  
  .tabs-content {
    padding: 10px;
    min-height: 200px;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .custom-tabs {
    .tabs-header {
      .tab-item {
        padding: 10px 6px;
        
        .tab-title {
          font-size: 14px;
        }
        
        .tab-subtitle {
          font-size: 11px;
        }
      }
    }
    
    .tabs-content {
      padding: 5px;
    }
  }
}

/* 绿钻主题变体 */
.custom-tabs.green-diamond-theme {
  .tabs-header {
    background: linear-gradient(135deg, #000000 0%, #8B4513 100%);
  }
}

/* 情侣会员主题变体 */
.custom-tabs.lover-theme {
  .tabs-header {
    background: linear-gradient(135deg, #ff6b6b 0%, #ffd93d 100%);
  }
}

/* 听书会员主题变体 */
.custom-tabs.audio-theme {
  .tabs-header {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }
}
</style>
