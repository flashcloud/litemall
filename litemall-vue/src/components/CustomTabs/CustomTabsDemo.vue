<template>
  <div class="member-tabs-demo">
    <van-nav-bar 
      title="会员方案"
      left-arrow
      @click-left="$router.go(-1)"
      class="nav-bar"
    />
    
    <!-- 使用自定义Tabs组件 -->
    <custom-tabs
      v-model="activeTab"
      :tabs="memberTabs"
      class="green-diamond-theme"
      @change="handleTabChange"
    >
      <template v-slot="{ currentTab, activeTabData }">
        <div class="tab-content-area">
          <!-- 超级会员内容 -->
          <div v-if="currentTab === 0" class="member-content">
            <div class="member-header">
              <van-icon name="diamond" class="member-icon" />
              <h3>{{ activeTabData.title }}</h3>
              <p>{{ activeTabData.description }}</p>
            </div>
            
            <div class="benefits-grid">
              <div class="benefit-item" v-for="(benefit, index) in superMemberBenefits" :key="index">
                <van-icon :name="benefit.icon" class="benefit-icon" />
                <span>{{ benefit.name }}</span>
              </div>
            </div>
            
            <div class="price-section">
              <div class="price-main">
                <span class="currency">¥</span>
                <span class="amount">15</span>
                <span class="period">/月</span>
              </div>
              <div class="price-original">原价 ¥40</div>
            </div>
          </div>
          
          <!-- 情侣会员内容 -->
          <div v-if="currentTab === 1" class="member-content">
            <div class="member-header">
              <van-icon name="heart" class="member-icon lover-icon" />
              <h3>{{ activeTabData.title }}</h3>
              <p>{{ activeTabData.description }}</p>
            </div>
            
            <div class="benefits-grid">
              <div class="benefit-item" v-for="(benefit, index) in loverMemberBenefits" :key="index">
                <van-icon :name="benefit.icon" class="benefit-icon" />
                <span>{{ benefit.name }}</span>
              </div>
            </div>
            
            <div class="price-section">
              <div class="price-main">
                <span class="currency">¥</span>
                <span class="amount">20</span>
                <span class="period">/月</span>
              </div>
              <div class="price-original">原价 ¥35</div>
            </div>
          </div>
          
          <!-- 听书会员内容 -->
          <div v-if="currentTab === 2" class="member-content">
            <div class="member-header">
              <van-icon name="volume" class="member-icon audio-icon" />
              <h3>{{ activeTabData.title }}</h3>
              <p>{{ activeTabData.description }}</p>
            </div>
            
            <div class="benefits-grid">
              <div class="benefit-item" v-for="(benefit, index) in audioMemberBenefits" :key="index">
                <van-icon :name="benefit.icon" class="benefit-icon" />
                <span>{{ benefit.name }}</span>
              </div>
            </div>
            
            <div class="price-section">
              <div class="price-main">
                <span class="currency">¥</span>
                <span class="amount">12</span>
                <span class="period">/月</span>
              </div>
              <div class="price-original">原价 ¥25</div>
            </div>
          </div>
        </div>
      </template>
    </custom-tabs>
    
    <!-- 底部操作按钮 -->
    <div class="action-footer">
      <van-button 
        type="primary" 
        size="large"
        @click="handleSubscribe"
        class="subscribe-btn"
      >
        立即开通 {{ memberTabs[activeTab].title }}
      </van-button>
      
      <div class="agreement-text">
        开通即同意 <span class="link">《会员服务协议》</span>
      </div>
    </div>
  </div>
</template>

<script>
import CustomTabs from '@/components/CustomTabs/index.vue';
import { NavBar, Button, Icon, Toast } from 'vant';

export default {
  name: 'MemberTabsDemo',
  components: {
    CustomTabs,
    [NavBar.name]: NavBar,
    [Button.name]: Button,
    [Icon.name]: Icon
  },
  data() {
    return {
      activeTab: 0,
      memberTabs: [
        {
          title: '超级会员',
          subtitle: '限时福利',
          badge: 'HOT',
          description: '尊享50+顶级权益，畅享豪华绿钻体验'
        },
        {
          title: '情侣会员',
          subtitle: '二人开 两人享',
          description: '专为情侣设计的专属会员服务'
        },
        {
          title: '听书会员',
          subtitle: '万本有声专辑',
          description: '海量有声读物，随时随地畅听'
        }
      ],
      superMemberBenefits: [
        { name: '豪华绿钻', icon: 'diamond' },
        { name: '听书会员', icon: 'volume' },
        { name: '视频VIP', icon: 'play-circle' },
        { name: '音乐包月', icon: 'music' },
        { name: '游戏特权', icon: 'gift' },
        { name: '购物优惠', icon: 'shopping-cart' }
      ],
      loverMemberBenefits: [
        { name: '情侣空间', icon: 'heart' },
        { name: '专属标识', icon: 'star' },
        { name: '双人特权', icon: 'friends' },
        { name: '纪念日提醒', icon: 'calendar' }
      ],
      audioMemberBenefits: [
        { name: '无损音质', icon: 'volume' },
        { name: '离线下载', icon: 'down' },
        { name: '专属书单', icon: 'bookmark' },
        { name: '倍速播放', icon: 'play' }
      ]
    };
  },
  methods: {
    handleTabChange(tabIndex, tabData) {
      console.log('切换到标签页:', tabIndex, tabData);
      Toast(`切换到${tabData.title}`);
    },
    handleSubscribe() {
      const currentMember = this.memberTabs[this.activeTab];
      Toast.success(`正在开通${currentMember.title}...`);
      // 这里可以调用开通会员的API
    }
  }
};
</script>

<style lang="scss" scoped>
.member-tabs-demo {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f9fa 0%, #e9ecef 100%);
  
  .nav-bar {
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  .tab-content-area {
    .member-content {
      .member-header {
        text-align: center;
        margin-bottom: 24px;
        
        .member-icon {
          font-size: 48px;
          color: #11998e;
          margin-bottom: 12px;
          
          &.lover-icon {
            color: #ff6b6b;
          }
          
          &.audio-icon {
            color: #4facfe;
          }
        }
        
        h3 {
          font-size: 24px;
          font-weight: 600;
          color: #333;
          margin: 8px 0;
        }
        
        p {
          font-size: 14px;
          color: #666;
          line-height: 1.5;
        }
      }
      
      .benefits-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
        margin-bottom: 32px;
        
        .benefit-item {
          display: flex;
          align-items: center;
          padding: 12px;
          background: #fff;
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
          
          .benefit-icon {
            font-size: 20px;
            color: #11998e;
            margin-right: 8px;
          }
          
          span {
            font-size: 14px;
            color: #333;
            font-weight: 500;
          }
        }
      }
      
      .price-section {
        text-align: center;
        padding: 20px;
        background: #fff;
        border-radius: 12px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        
        .price-main {
          display: flex;
          align-items: baseline;
          justify-content: center;
          margin-bottom: 8px;
          
          .currency {
            font-size: 16px;
            color: #ff4757;
            font-weight: 500;
          }
          
          .amount {
            font-size: 36px;
            color: #ff4757;
            font-weight: 700;
            margin: 0 4px;
          }
          
          .period {
            font-size: 16px;
            color: #666;
          }
        }
        
        .price-original {
          font-size: 14px;
          color: #999;
          text-decoration: line-through;
        }
      }
    }
  }
  
  .action-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: #fff;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
    
    .subscribe-btn {
      width: 100%;
      height: 48px;
      border-radius: 24px;
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      border: none;
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 12px;
    }
    
    .agreement-text {
      text-align: center;
      font-size: 12px;
      color: #999;
      
      .link {
        color: #11998e;
        text-decoration: underline;
      }
    }
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .member-tabs-demo {
    .tab-content-area {
      .member-content {
        .benefits-grid {
          grid-template-columns: 1fr;
          gap: 12px;
        }
        
        .member-header {
          .member-icon {
            font-size: 40px;
          }
          
          h3 {
            font-size: 20px;
          }
        }
      }
    }
  }
}
</style>
