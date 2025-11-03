<template>
  <div class="bind-phone-container">
    <!-- 头部标题 -->
    <div class="header">
      <h1 class="title">绑定企业用户</h1>
      <p class="subtitle">必须至少绑定一家企业用户信息，</p>
      <p class="subtitle">请输入正确的企业全称及统一社会信用代码</p>
    </div>

    <!-- 表单区域 -->
    <div class="form-container">
        <!-- 手机号输入框 -->
        <custom-input
        v-model="traderName"
        type="text"
        placeholder="请输入正确的企业全称[必填]"
        maxlength="50"
        icon="arrow"
        icon-class="phone-icon"
        />

        <custom-input
        v-model="traderTaxId"
        type="text"
        placeholder="请输入统一社会信用代码[选填]"
        hint="统一社会信用代码可以不填写。如果需要填写，必须为18位"
        maxlength="18"
        icon="arrow"
        icon-class="phone-icon"
        />
        

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
  </div>
</template>

<script>
import {getWeChatCode, authCaptcha, bindTrader } from '@/api/api';
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
      traderName: '',
      traderTaxId: '', //统一社会信用代码,请使用18位随机数预生成
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
    isTaxCodeValid() {
        if (this.traderTaxId.length > 0) {
            return /^[0-9A-Z]{18}$/.test(this.traderTaxId)
        }
        return true;
    },
    canSubmit() {
      return this.isTaxCodeValid && 
             this.traderName.length > 0;
    }
  },
  methods: {
    fillRandomTaxCode() {
        // 统一社会信用代码,请使用18位随机数预生成
        const chars = '0123456789'
        let code = ''
        for (let i = 0; i < 18; i++) {
            code += chars.charAt(Math.floor(Math.random() * chars.length))
        }
        return code
    },

    async confirmBind() {
      if (!this.canSubmit) return

      if (this.traderTaxId.length > 0 && this.traderTaxId.length < 18) {
        Toast.fail('统一社会信用代码格式错误,应为18位');
        return;
     }
      
      try {
        bindTrader({
            name: this.traderName,
            taxid: this.traderTaxId == null || this.traderTaxId.length === 0 ? this.fillRandomTaxCode() : this.traderTaxId,
            token: this.token
        }).then(res => {
            this.$router.push({ name: 'user' })
            this.$dialog.alert({ message: '绑定企业用户成功' }).then(() => {

            });
        }).catch(error => {
            Toast.fail('绑定失败: ' + error.data.errmsg)
        });
      } catch (error) {
        Toast.fail('绑定失败: ' + error.toString())
      }
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
  color: #333;
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