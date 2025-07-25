<template>
  <div class="member-center">
    <van-nav-bar title="会员中心" v-if="!isMobile" left-arrow @click-left="onBack" />
    
    <!-- 用户信息区域 -->
    <div class="user-section">
      <div class="user-avatar">
        <img :src="avatar" alt="用户头像" />
      </div>
      <div class="user-info">
        <div class="username">{{nickName}}</div>
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
              <p class="member-description">{{ activeTabData ? activeTabData.description : '' }}</p>
            </div>
            
            <!-- 会员套餐选择组件 -->
            <membership-plans
              v-model="selectedPlan.planKey"
              :plans="memberTabs[currentTab] ? memberTabs[currentTab].planList : []"
              @plan-change="handlePlanChange"
            />
          </div>
          
          <!-- 云端会员内容 -->
          <div v-if="currentTab === 1" class="member-content">
            <!-- <div class="member-header">
              <p class="member-description">{{activeTabData ? activeTabData.description : ''}}</p>
            </div> -->
            
            <!-- 美观的建设中页面 -->
            <div class="under-construction">
              <div class="construction-icon">
                <div class="icon-wrapper">
                  <van-icon name="setting-o" size="60" />
                  <div class="rotating-gear"></div>
                </div>
              </div>
              
              <div class="construction-content">
                <h3 class="construction-title">云端会员功能</h3>
                <p class="construction-subtitle">正在精心建设中</p>
                <div class="construction-description">
                  <p>我们正在为您打造更强大的云端体验</p>
                  <p>敬请期待即将到来的惊喜功能</p>
                </div>
                
                <div class="construction-features">
                  <div class="feature-item">
                    <van-icon name="cloud-o" />
                    <span>云端同步</span>
                  </div>
                  <div class="feature-item">
                    <van-icon name="share-o" />
                    <span>多端协作</span>
                  </div>
                  <div class="feature-item">
                    <van-icon name="shield-o" />
                    <span>安全保障</span>
                  </div>
                </div>
                
                <div class="progress-bar">
                  <div class="progress-fill"></div>
                </div>
                <p class="progress-text">开发进度 75%</p>
              </div>
            </div>

            <!-- 金软云端会员套餐选择组件 -->
            <!-- <membership-plans
              v-model="selectedPlan.planKey"
              :plans="memberTabs[currentTab].planList"
              @plan-change="handlePlanChange"
            /> -->
          </div>

        </div>

        <!-- 连续包月说明 -->
        <div class="auto-renew-note">
            连续包月/年：到期自动续费，可随时取消 <van-icon name="question-o" />
        </div>

        <!-- 确认支付按钮 -->
        <div class="payment-section">
            <van-button 
            :disabled="!(selectedPlan.planKey)"
            type="primary" 
            size="large" 
            :loading="isLoading"
            @click="confirmPayment"
            class="pay-btn"
            >
            {{ selectedPlan.planKey ? `确认协议并支付¥${getCurrentPrice()}` : '请选择套餐' }}
            
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
import MembershipPlans from '@/components/CustomTabs/MembershipPlans/index.vue';

import { setLocalStorage, removeLocalStorage } from '@/utils/local-storage';
import { authInfo, authLoginByAccount, authLogout, getMemberList, authProfile } from '@/api/api';

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
    MembershipPlans,
    [NavBar.name]: NavBar,
    [Button.name]: Button,
    [Icon.name]: Icon,
    [Checkbox.name]: Checkbox
  },

    created() {
        this.initViews();
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
      membersInfo: null,
      memberTabs: [],
      agreeTerms: false,
      isLoading: false,
      planPrices: {},
      selectedPlan: {
        planKey: '',
        price: 0
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
    initViews() {
      getMemberList().then(res => {
        this.membersInfo = res.data.data;

        //构造套餐配置数据
        const buildPlans = plan => {
            const title = plan.specifications[0];
            let key = '';
            const goodsId = plan.goodsId
            let note = '';
            let tag = '';
            let planType = '';
            switch (title) {
                case '连续包年':
                    key = 'yearly-auto';
                    planType = 'yearly-auto-card';
                    tag = '限时特惠';
                    break;
                case '连续包月':
                    key = 'monthly';
                    planType = 'new-friend-offer';
                    note = '次月续费25元';
                    tag = '新朋友特惠';
                    break;
                case '年卡':
                    key = 'yearly';
                    planType = 'limited-offer';
                    break;
                case '1个月':
                    key = 'monthly-single';
                    planType = 'monthly-card';
                    break;
                default:
                    key = '';
                    note = '';
                    tag = '';
            }

            if(key === '') {
                Toast.fail('未知的套餐类型: ' + title);
                console.error('未知的套餐类型:', title);
                return null; // 如果没有匹配的套餐类型，则返回null
            }
            key = `plan-${goodsId}-${key}`;

            this.planPrices[key] = plan.price;
            if (!this.selectedPlan) {
                // 如果没有选中套餐，则设置默认选中，TODO：目前下面的操作无效，只能暂时硬编码“plan-1-yearly-auto”
                //this.$set(this.selectedPlan, "name" , key);
                //this.$set(this.selectedPlan, "price" , plan.price);
            }

            //构造套餐数据
            return {
                id: plan.id,
                goodsId: goodsId,
                key: key,
                title: title,
                currentPrice: plan.price,
                originalPrice: Math.floor(plan.price * 1.2), //原价是当前价格的八折，并且保留整数
                tag: tag,
                note: note,
                planType: planType
            };
        };
        
        //构造会员标签数据
        this.memberTabs = this.membersInfo.memberList.map(member => {
            const memberTabData = {
                title: member.name,
                subtitle: member.brief.split("｜")[0],
                description: member.brief.indexOf("｜") > -1 ? member.brief.split("｜")[1] : '',
                badge: member.isHot === true ? 'HOT' : '',
                id: member.id,
                planList: []
            };
            //找到会员对应的套餐
            this.membersInfo.productList.forEach(plan => {
                if (plan.goodsId === member.id) {
                    const planData = buildPlans(plan);
                    memberTabData.planList.push(planData);
                }
            });
            return memberTabData;
        });
      });
    },
    onBack() {
      this.$router.go(-1);
    },
    getCurrentPrice() {
      return this.planPrices[this.selectedPlan.planKey] || 0;
    },
    handleTabChange(tabIndex, tabData) {
      //console.log('切换到标签页:', tabIndex, tabData);
      //Toast(`切换到${tabData.title}`);
      this.selectedPlan.planKey = ''; // 重置选中的套餐
    },
    handleSubscribe() {
      const currentMember = this.memberTabs[this.activeTab];
      Toast.success(`正在开通${currentMember.title}...`);
      // 这里可以调用开通会员的API
    },    
    async confirmPayment() {
      if (!this.selectedPlan.planKey) {
        Toast('请选择一个套餐');
        return;
      }

      if (!this.agreeTerms) {
        Toast('请先阅读并同意相关协议');
        return;
      }

      //弹出Confirm对话框确认选择的套餐
      
      this.isLoading = true;
      try {
        const price = this.getCurrentPrice();
        console.log('确认支付套餐:', this.selectedPlan.planKey, '价格:', price);
        
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
    },
    handlePlanChange(planData) {
        this.selectedPlan = planData;
        console.log('选择的套餐:',  planData);
    },
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
  color: white;
  margin-top: 8px;
  background-color: black;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #ffeaa7;
}

.green-diamond-theme {
  margin: 0px 10px 10px 10px;
}

// 正在建设中样式
.under-construction {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5e6a3 0%, #f7e7a4 100%);
  border-radius: 20px;
  margin: 5px;
  color: #333;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(245, 230, 163, 0.3);
  animation: containerPulse 4s infinite ease-in-out;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.4) 0%, transparent 70%);
    animation: shimmer 3s infinite linear;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent);
    animation: sweep 6s infinite ease-in-out;
  }

  .construction-icon {
    position: relative;
    margin-bottom: 20px;
    z-index: 2;
    animation: bounceIn 2s ease-out;

    .icon-wrapper {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 120px;
      height: 120px;
      background: rgba(255, 255, 255, 0.4);
      border-radius: 50%;
      backdrop-filter: blur(10px);
      border: 2px solid rgba(255, 255, 255, 0.6);
      box-shadow: 0 8px 24px rgba(245, 230, 163, 0.5);
      animation: iconFloat 3s infinite ease-in-out;

      &:hover {
        animation: iconHover 0.5s ease-in-out;
      }

      .van-icon {
        color: #666;
        filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
        animation: iconPulse 2s infinite ease-in-out;
      }

      .rotating-gear {
        position: absolute;
        width: 100%;
        height: 100%;
        border-radius: 50%;
        border: 3px dashed rgba(255, 255, 255, 0.6);
        animation: rotate 6s infinite linear;
      }

      // 添加多个装饰环
      &::before {
        content: '';
        position: absolute;
        width: 140px;
        height: 140px;
        border: 2px dotted rgba(255, 255, 255, 0.3);
        border-radius: 50%;
        animation: rotate 10s infinite linear reverse;
      }

      &::after {
        content: '';
        position: absolute;
        width: 160px;
        height: 160px;
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 50%;
        animation: rotate 15s infinite linear;
      }
    }
  }

  .construction-content {
    text-align: center;
    z-index: 2;

    .construction-title {
      font-size: 28px;
      font-weight: bold;
      margin-bottom: 8px;
      background: linear-gradient(45deg, #333, #666);
      background-clip: text;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      text-shadow: none;
      animation: titleSlideIn 1.5s ease-out;
    }

    .construction-subtitle {
      font-size: 18px;
      margin-bottom: 20px;
      opacity: 0.8;
      font-weight: 300;
      color: #555;
      animation: subtitleFadeIn 2s ease-out 0.5s both;
    }

    .construction-description {
      margin-bottom: 30px;
      animation: descriptionSlideUp 2s ease-out 1s both;
      
      p {
        margin: 5px 0;
        font-size: 14px;
        opacity: 0.7;
        line-height: 1.5;
        color: #666;
        animation: textWave 3s infinite ease-in-out;
        
        &:nth-child(2) {
          animation-delay: 0.5s;
        }
      }
    }

    .construction-features {
      display: flex;
      justify-content: center;
      gap: 30px;
      margin-bottom: 30px;
      flex-wrap: wrap;

      .feature-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
        padding: 15px;
        background: rgba(255, 255, 255, 0.6);
        border-radius: 12px;
        backdrop-filter: blur(5px);
        border: 1px solid rgba(255, 255, 255, 0.8);
        min-width: 80px;
        transition: all 0.3s ease;
        box-shadow: 0 2px 8px rgba(245, 230, 163, 0.3);
        animation: featureSlideIn 1.5s ease-out both;

        &:nth-child(1) { animation-delay: 1.5s; }
        &:nth-child(2) { animation-delay: 1.7s; }
        &:nth-child(3) { animation-delay: 1.9s; }

        &:hover {
          transform: translateY(-5px) scale(1.05);
          box-shadow: 0 8px 20px rgba(245, 230, 163, 0.5);
          animation: featureHover 0.6s ease-in-out;
        }

        .van-icon {
          font-size: 24px;
          color: #666;
          animation: iconBob 2s infinite ease-in-out;
        }

        span {
          font-size: 12px;
          opacity: 0.8;
          color: #555;
        }
      }
    }

    .progress-bar {
      width: 200px;
      height: 8px;
      background: rgba(255, 255, 255, 0.6);
      border-radius: 4px;
      margin: 0 auto 10px;
      overflow: hidden;
      box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
      animation: progressBarSlideIn 2s ease-out 2.5s both;

      .progress-fill {
        width: 75%;
        height: 100%;
        background: linear-gradient(90deg, #e6a23c 0%, #f39c12 100%);
        border-radius: 4px;
        animation: progressGrow 3s ease-out 3s both, progressShine 2s infinite ease-in-out 6s;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8), transparent);
          animation: progressSweep 2s infinite linear 6s;
        }
      }
    }

    .progress-text {
      font-size: 12px;
      opacity: 0.7;
      margin: 0;
      color: #666;
      animation: progressTextFadeIn 1s ease-out 3.5s both;
    }
  }
}

// 动画关键帧定义
@keyframes containerPulse {
  0%, 100% { box-shadow: 0 4px 16px rgba(245, 230, 163, 0.3); }
  50% { box-shadow: 0 8px 24px rgba(245, 230, 163, 0.5); }
}

@keyframes shimmer {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes sweep {
  0% { left: -100%; }
  50% { left: 100%; }
  100% { left: 100%; }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes bounceIn {
  0% {
    transform: scale(0.3) translateY(-50px);
    opacity: 0;
  }
  50% {
    transform: scale(1.1) translateY(-10px);
    opacity: 0.8;
  }
  100% {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}

@keyframes iconFloat {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

@keyframes iconHover {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes iconPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

@keyframes iconBob {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-3px); }
}

@keyframes titleSlideIn {
  0% {
    transform: translateY(-30px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes subtitleFadeIn {
  0% {
    transform: translateY(20px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 0.8;
  }
}

@keyframes descriptionSlideUp {
  0% {
    transform: translateY(30px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes textWave {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-2px); }
}

@keyframes featureSlideIn {
  0% {
    transform: translateY(50px) scale(0.8);
    opacity: 0;
  }
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

@keyframes featureHover {
  0%, 100% { transform: translateY(-5px) scale(1.05); }
  50% { transform: translateY(-8px) scale(1.08); }
}

@keyframes progressBarSlideIn {
  0% {
    transform: translateX(-100px);
    opacity: 0;
  }
  100% {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes progressGrow {
  0% { width: 0%; }
  100% { width: 75%; }
}

@keyframes progressShine {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

@keyframes progressSweep {
  0% { left: -100%; }
  100% { left: 100%; }
}

@keyframes progressTextFadeIn {
  0% {
    transform: translateY(10px);
    opacity: 0;
  }
  100% {
    transform: translateY(0);
    opacity: 0.7;
  }
}
</style>