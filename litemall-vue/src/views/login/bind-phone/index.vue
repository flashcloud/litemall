<template>
  <div class="bind-phone-container">
    <!-- 头部标题 -->
    <div class="header">
      <h1 class="title">绑定手机号</h1>
      <p class="subtitle">为保证您的账户安全，请及时绑定手机号</p>
    </div>

    <!-- 表单区域 -->
    <div class="form-container">
        <!-- 手机号输入框 -->
        <custom-input
        v-model="phoneNumber"
        type="tel"
        placeholder="请输入手机号"
        maxlength="11"
        icon="mobile"
        icon-class="phone-icon"
        />

        <!-- 验证码输入框 -->
        <custom-input
        v-model="verifyCode"
        type="text"
        placeholder="请输入验证码"
        maxlength="6"
        icon="lock"
        icon-class="lock-icon"
        :show-button="true"
        :button-text="countDownText"
        :button-disabled="isCountingDown || !isPhoneValid"
        @button-click="getVerifyCode"
        />
      
      <!-- 协议同意 -->
      <div class="agreement-section">
        <label class="agreement-label">
          <input
            v-model="agreeTerms"
            type="checkbox"
            class="agreement-checkbox"
          />
          <span class="checkmark"></span>
          <span class="agreement-text">
            我已阅读并同意了
            <a href="#" class="link">用户协议</a>
            和
            <a href="#" class="link">隐私政策</a>
          </span>
        </label>
      </div>

      <!-- 确认按钮 -->
      <custom-button
        text="确定绑定"
        :disabled="!canSubmit"
        variant="primary"
        size="large"
        block
        @click="confirmBind"
      />      
    </div>

    <!-- 说明区域 -->
    <div class="info-section">
      <h3 class="info-title">为什么需要绑定手机号？</h3>
      <div class="info-item">
        <span class="info-number">1.</span>
        <span class="info-text">
          依据《网络安全法》，互联网注册用户要提供基于移动电话号码等方式的验证。
        </span>
      </div>
      <div class="info-item">
        <span class="info-number">2.</span>
        <span class="info-text">
          绑定手机号后，可直接使用手机号和验证码登录，更加灵活和安全。
        </span>
      </div>
      <div class="info-item">
        <span class="info-number">3.</span>
        <span class="info-text">
          绑定手机号后，即使第三方登录出现故障，也可以通过手机号顺利登录。
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import {getWeChatCode, authCaptcha, manualBindPhone } from '@/api/api';
import { getLocalStorage } from '@/utils/local-storage';
import { Toast } from 'vant';
import CustomInput from '@/components/cust-form/CustomInput';
import CustomButton from '@/components/cust-form/CustomButton';

function isWeChatBrowser() {
    var ua = navigator.userAgent.toLowerCase();
    return ua.indexOf('micromessenger') !== -1;
}

export default {
  name: 'BindPhone',

  components: {
    Toast,
    CustomInput,
    CustomButton
  },

  data() {
    return {
      token: '',
      phoneNumber: '',
      verifyCode: '',
      agreeTerms: false,
      isCountingDown: false,
      countDown: 60,
      timer: null
    }
  },

  created() {
      const infoData = getLocalStorage(
        'nickName',
        'avatar',
        'Authorization',
        'memberType',
        'memberPlan',
        'memberExpire'
      );
      this.token = infoData.Authorization;
  },

  computed: {
    isPhoneValid() {
      return /^1[3-9]\d{9}$/.test(this.phoneNumber)
    },
    countDownText() {
      return this.isCountingDown ? `${this.countDown}s` : '获取验证码'
    },
    canSubmit() {
      return this.isPhoneValid && 
             this.verifyCode.length === 6 && 
             this.agreeTerms
    }
  },
  methods: {
    async getVerifyCode() {
      if (!this.isPhoneValid) {
        this.$message.warning('请输入正确的手机号')
        return
      }

        authCaptcha({
            mobile: this.phoneNumber,
            type: 'bind-mobile'
        }).then(() => {
            Toast.success('验证码已发送');
            this.startCountDown()
        }).catch(error => {
            Toast.fail("验证码发送失败，请重试: " + error.data.errmsg);
            this.resetCountDown();
        });
    },
    
    startCountDown() {
      this.isCountingDown = true
      this.countDown = 60
      
      this.timer = setInterval(() => {
        this.countDown--
        if (this.countDown <= 0) {
          this.isCountingDown = false
          clearInterval(this.timer)
        }
      }, 1000)
    },

    //恢复倒计时
    resetCountDown() {
      this.isCountingDown = false
      this.countDown = 60
      if (this.timer) {
        clearInterval(this.timer)
      }
    },

    async confirmBind() {
      if (!this.canSubmit) return
      
      try {
        manualBindPhone({
            phone: this.phoneNumber,
            code: this.verifyCode,
            token: this.token
        }).then(res => {
            this.$router.push({ name: 'user' })
            this.$dialog.alert({ message: '手机绑定成功。您也可以在App中使用手机号登录，初始密码是手机号后6位。强烈建议登录后修改该密码' }).then(() => {

            });
        }).catch(error => {
            Toast.fail('绑定失败: ' + error.data.errmsg)
        });
      } catch (error) {
        Toast.fail('绑定失败，请检查验证码是否正确')
      }
    }
  },
  
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  }
}
</script>

<style scoped>
.bind-phone-container {
  min-height: 100vh;
  background: #f8f9fa;
  padding: 40px 20px 20px;
  box-sizing: border-box;
}

.header {
  text-align: center;
  margin-bottom: 60px;
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #017065;
  margin: 0 0 16px 0;
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
  line-height: 1.5;
}

.form-container {
  max-width: 400px;
  margin: 0 auto;
}

.agreement-section {
  margin: 30px 0;
  text-align: center;
}

.agreement-label {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.agreement-checkbox {
  display: none;
}

.checkmark {
  width: 16px;
  height: 16px;
  border: 1px solid #ddd;
  border-radius: 3px;
  margin-right: 8px;
  position: relative;
  background: white;
}

.agreement-checkbox:checked + .checkmark {
  background: #029688;
  border-color: #029688;
}

.agreement-checkbox:checked + .checkmark::after {
  content: '✓';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 10px;
}

.agreement-text {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.link {
  color: #029688;
  text-decoration: none;
}

.info-section {
  max-width: 400px;
  margin: 40px auto;
}

.info-title {
  font-size: 12px;
  font-weight: 600;
  color: #999;
  margin: 0 0 20px 0;
}

.info-item {
  display: flex;
  margin-bottom: 16px;
  line-height: 1.6;
}

.info-number {
  font-size: 14px;
  color: #999;
  margin-right: 8px;
  flex-shrink: 0;
}

.info-text {
  font-size: 12px;
  color: #999;
}

@media (max-width: 480px) {
  .bind-phone-container {
    padding: 20px 30px;
  }
  
  .title {
    font-size: 24px;
  }
  
  .form-container,
  .info-section {
    max-width: none;
  }
}
</style>