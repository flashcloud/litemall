<template>
  <div class="member-center">
    <van-nav-bar title="会员中心" v-if="!isMobile" left-arrow @click-left="onBack" />
    
    <!-- 用户信息区域 -->
    <div class="user-section">
      <div class="user-avatar">
        <img :src="avatar" alt="用户头像" />
      </div>
      <div class="user-info">
        <div class="username">{{nickName}}
            <!-- <van-button size="small" class="user_quit" @click="loginOut">退出当前账户</van-button> -->
        </div>
        <div class="member-desc">加入会员享受金软专属权益</div>
      </div>
    </div>


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
              <!-- <van-icon name="diamond" class="member-icon" /> -->
              <!-- <h3>{{ activeTabData.title }}</h3> -->
              <p class="member-description">{{ activeTabData.description }}</p>
            </div>
            
            <!-- 会员套餐选择 -->
            <div class="membership-plans">
            <div class="plan-row">
                <!-- 限时特惠 - 年卡 -->
                <div 
                class="plan-card limited-offer"
                :class="{ active: selectedPlan === 'yearly' }"
                @click="selectPlan('yearly')"
                >
                <div class="plan-tag">限时特惠</div>
                <div class="plan-title">年卡</div>
                <div class="plan-price">
                    <span class="current-price">¥420</span>
                </div>
                <div class="original-price">¥480</div>
                </div>

                <!-- 新朋友特惠 - 连续包月 -->
                <div 
                class="plan-card new-friend-offer"
                :class="{ active: selectedPlan === 'monthly' }"
                @click="selectPlan('monthly')"
                >
                <div class="plan-tag">新朋友特惠</div>
                <div class="plan-title">连续包月</div>
                <div class="plan-price">
                    <span class="current-price">¥25</span>
                </div>
                <div class="original-price">¥30</div>
                <div class="plan-note">次月续费25元</div>
                </div>
            </div>

            <div class="plan-row">
                <!-- 连续包年 -->
                <div 
                class="plan-card yearly-auto-card"
                :class="{ active: selectedPlan === 'yearly-auto' }"
                @click="selectPlan('yearly-auto')"
                >
                <div class="plan-title">连续包年</div>
                <div class="plan-price">
                    <span class="current-price">¥263</span>
                </div>
                <div class="original-price">¥298</div>
                </div>

                <!-- 月卡 -->
                <div class="plan-card monthly-card"
                :class="{ active: selectedPlan === 'monthly-single' }"
                @click="selectPlan('monthly-single')"
                >
                <div class="plan-title">1个月</div>
                <div class="plan-price">
                    <span class="current-price">¥40</span>
                </div>
                </div>
            </div>
            </div>

            <!-- 连续包月说明 -->
            <div class="auto-renew-note">
            连续包月：到期按每月25元自动续费，可随时取消 <van-icon name="question-o" />
            </div>

            <!-- 确认支付按钮 -->
            <div class="payment-section">
            <van-button 
                type="primary" 
                size="large" 
                :loading="isLoading"
                @click="confirmPayment"
                class="pay-btn"
            >
                确认协议并支付¥{{ getCurrentPrice() }}
            </van-button>
            
            <!-- 协议条款 -->
            <div class="agreement">
                <van-checkbox v-model="agreeTerms" size="14px">
                开通前阅读
                </van-checkbox>
                <span class="agreement-link" @click="showAgreement">《会员服务协议》</span>
                <span class="agreement-link" @click="showAutoRenewAgreement">《自动续费声明》</span>
            </div>
            </div>
          </div>
          
          <!-- 情侣会员内容 -->
          <div v-if="currentTab === 1" class="member-content">
            <div class="member-header">
              <!-- <van-icon name="heart" class="member-icon lover-icon" /> -->
              <!-- <h3>{{ activeTabData.title }}</h3> -->
              <p class="member-description">{{ activeTabData.description }}</p>
            </div>

            <p style="text-align: center;">努力建设中...</p>
          </div>
        </div>
      </template>
    </custom-tabs>





    
  </div>
</template>

<script>
import { NavBar, Button, Icon, Checkbox, Toast } from 'vant';

import avatar_default from '@/assets/images/avatar_default.png';
import bg_default from '@/assets/images/user_head_bg.png';
import { getLocalStorage } from '@/utils/local-storage';
import CustomTabs from '@/components/CustomTabs/index.vue';

import { setLocalStorage, removeLocalStorage } from '@/utils/local-storage';
import { authInfo, authLoginByAccount, authLogout, authProfile } from '@/api/api';

export default {
  name: 'member-center',

  props: {
    isLogin: {
      type: Boolean,
      default: false
    },
  },

  components: {
    CustomTabs,
    [NavBar.name]: NavBar,
    [Button.name]: Button,
    [Icon.name]: Icon,
    [Checkbox.name]: Checkbox
  },

    created() {

    },

    mounted() {
        this.$nextTick(() => {
            // 检查是否是移动端访问
            this.isMobile = this.$route.query.isMobile || false;
            if (this.isMobile) {
                // 如果是移动端，获取登录信息
                this.userName = this.$route.query.userName || '';
                this.password = this.$route.query.password || '';
                
                if (this.userName && this.password) {
                    this.login();
                } else {
                    this.$router.push({ name: 'login' });
                }
            } else {
                // 如果是PC端，获取用户信息
                this.getUserInfo();
            }
        });
    },

  data() {
    return {
      userName: '',
      password: '',
      isMobile: false,

      nickName: '昵称',
      avatar: avatar_default,
      token: '',
      background_image: bg_default,

      activeTab: 0,
      memberTabs: [
        {
          title: 'WI-FI会员',
          subtitle: '本地使用',
          badge: 'HOT',
          description: '注意！移动设备需要与PC端金软是同一WI-FI',
        },
        {
          title: '云端会员',
          subtitle: '云端使用',
          description: '任何时候任何地方轻松连接到企业的PC端金软'
        }
      ],

      selectedPlan: 'monthly', // 默认选择连续包月
      agreeTerms: false,
      isLoading: false,
      planPrices: {
        'yearly': 420,
        'monthly': 25,
        'yearly-auto': 263,
        'monthly-single': 40
      }
    };
  },
  methods: {
    login() {
      let loginData = {username: this.userName, password: this.password};
      
      authLoginByAccount(loginData).then(res => {
        const userInfo = res.data.data.userInfo;
        setLocalStorage({
          Authorization: res.data.data.token,
          avatar: userInfo.avatarUrl,
          nickName: userInfo.nickName
        });
        this.getUserInfo();
      }).catch(error => {
        Toast.fail(error.data.errmsg);
      });
    },
    loginOut() {
      authLogout().then(res => {
        removeLocalStorage('Authorization')
        removeLocalStorage('avatar')
        removeLocalStorage('nickName')
        this.$router.push({ name: 'home' });
      });
    },
    getUserInfo() {
      const infoData = getLocalStorage(
        'nickName',
        'avatar',
        'Authorization'
      );
      this.avatar = infoData.avatar || avatar_default;
      this.nickName = infoData.nickName || '昵称';
      this.token = infoData.Authorization || '';
      if (this.token === '') {
        this.$router.push({ name: 'login' });
        return;
      }

      if(this.avatar.indexOf('/images/avatar/') > -1 && this.token !== '') {
        this.avatar = '/wx/auth/getAvatar?token=' + this.token;
      }
    },
    onBack() {
      this.$router.go(-1);
    },
    selectPlan(planType) {
      this.selectedPlan = planType;
    },
    getCurrentPrice() {
      return this.planPrices[this.selectedPlan] || 12;
    },
    handleTabChange(tabIndex, tabData) {
      //console.log('切换到标签页:', tabIndex, tabData);
      //Toast(`切换到${tabData.title}`);
    },
    handleSubscribe() {
      const currentMember = this.memberTabs[this.activeTab];
      Toast.success(`正在开通${currentMember.title}...`);
      // 这里可以调用开通会员的API
    },    
    async confirmPayment() {
      if (!this.agreeTerms) {
        Toast('请先阅读并同意相关协议');
        return;
      }
      
      this.isLoading = true;
      try {
        const price = this.getCurrentPrice();
        console.log('确认支付套餐:', this.selectedPlan, '价格:', price);
        
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        Toast.success('支付成功！会员开通成功！');
        setTimeout(() => {
          this.$router.go(-1);
        }, 1500);
      } catch (error) {
        Toast.fail('支付失败，请重试');
      } finally {
        this.isLoading = false;
      }
    },
    showAgreement() {
      Toast('显示会员服务协议');
    },
    showAutoRenewAgreement() {
      Toast('显示自动续费声明');
    }
  }
};
</script>

<style scoped lang="scss">
.member-center {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.user-section {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background-color: #fff;
  margin-bottom: 5px;
  
  .user-avatar {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    overflow: hidden;
    margin-right: 16px;
    background-color: #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: center;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .user-info {
    .username {
      font-size: 18px;
      font-weight: bold;
      color: #333;
      margin-bottom: 4px;
    }
    
    .member-desc {
      font-size: 14px;
      color: #666;
    }
  }
}

.membership-plans {
  padding: 0 5px;
  margin-bottom: 16px;
  
  .plan-row {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
    
    .plan-card {
      flex: 1;
      background-color: #fff;
      border-radius: 12px;
      padding: 16px 12px;
      text-align: center;
      cursor: pointer;
      border: 2px solid transparent;
      position: relative;
      transition: all 0.3s ease;
      
      &.active {
        border-color: #ffd700;
        box-shadow: 0 0 10px rgba(255, 215, 0, 0.3);
      }
      
      &.limited-offer {
        background: linear-gradient(135deg, #ffe5e5 0%, #fff0f0 100%);
        
        .plan-tag {
          background-color: #DC143C;
          color: white;
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 10px;
          position: absolute;
          top: -6px;
          left: 50%;
          transform: translateX(-50%);
        }
      }
      
      &.new-friend-offer {
        background: linear-gradient(135deg, #f5e6a3 0%, #f7e7a4 100%);
        
        .plan-tag {
          background-color: #e6a23c;
          color: white;
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 10px;
          position: absolute;
          top: -6px;
          left: 50%;
          transform: translateX(-50%);
        }
        
        .plan-note {
          font-size: 11px;
          color: #666;
          margin-top: 4px;
        }
      }
      
      &.monthly-card {
        background: linear-gradient(135deg, #ffe5e5 0%, #fff0f0 100%);
      }

      &.yearly-auto-card {
        background: linear-gradient(135deg, #e6f7ff 0%, #f0faff 100%);
      }

      .plan-title {
        font-size: 16px;
        font-weight: bold;
        color: #333;
        margin-bottom: 8px;
        margin-top: 8px;
      }
      
      .plan-price {
        .current-price {
          font-size: 24px;
          font-weight: bold;
          color: #DC143C;
        }
      }
      
      .original-price {
        font-size: 12px;
        color: #999;
        text-decoration: line-through;
        margin-top: 4px;
      }
    }
  }
}

.auto-renew-note {
  padding: 0 16px;
  margin-bottom: 20px;
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  
  .van-icon {
    color: #999;
  }
}

.payment-section {
  padding: 0 16px 32px;
  
  .pay-btn {
    width: 100%;
    margin-bottom: 16px;
    height: 48px;
    background: linear-gradient(135deg, #f5e6a3 0%, #f7e7a4 100%);
    border: none;
    border-radius: 24px;
    font-size: 16px;
    font-weight: bold;
    color: #333;
  }
  
  .agreement {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #666;
    
    .van-checkbox {
      margin-right: 4px;
    }
    
    .agreement-link {
      color: #007aff;
      text-decoration: none;
      margin: 0 2px;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.member-description {
    font-size: 14px;
    // color: #666;
    color: white;
    margin-top: 8px;
    //background-color: #fff3cd;#DC143C
    background-color: black;
    // opacity: 0.7;
    padding: 12px 16px;
    border-radius: 8px;
    border: 1px solid #ffeaa7;
}


.green-diamond-theme {
    margin: 0px 10px 10px 10px;
}
</style>