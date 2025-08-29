<!-- 这个组件的主要特点：

可配置性强：

支持自定义输入类型、占位文本、最大长度等
图标可选，支持自定义图标类名
按钮可选，支持自定义按钮文本和禁用状态
事件处理：

使用 v-model 进行双向数据绑定
按钮点击事件通过 $emit 触发
样式优化：

保持了原有的视觉风格
响应式设计，适配不同屏幕尺寸
使用灵活：

可以单独作为输入框使用
也可以作为带操作按钮的输入框使用
你可以根据实际需求调整组件的样式和功能。 
-->
  
  <!-- 使用未例：
   <div>
    1.基础输入框
    <custom-input
      v-model="phoneNumber"
      type="tel"
      placeholder="请输入手机号"
      maxlength="11"
      icon="mobile"
      icon-class="phone-icon"
    />

    2.带按钮的输入框
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
  </div> -->

<template>
  <div class="custom-input">
    <div class="input-wrapper">
      <i v-if="icon" class="icon" :class="iconClass">
        <van-icon :name="icon" />
      </i>
      <input
        :value="value"
        @input="handleInput"
        :type="type"
        :placeholder="placeholder"
        :maxlength="maxlength"
        :disabled="disabled"
        class="input-field"
      />
      <button
        v-if="showButton"
        @click="handleButtonClick"
        :disabled="buttonDisabled"
        class="action-button"
      >
        {{ buttonText }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CustomInput',
  
  props: {
    // 输入框的值
    value: {
      type: [String, Number],
      default: ''
    },
    // 输入框类型
    type: {
      type: String,
      default: 'text'
    },
    // 占位文本
    placeholder: {
      type: String,
      default: ''
    },
    // 最大长度
    maxlength: {
      type: [String, Number],
      default: ''
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    },
    // 图标名称
    icon: {
      type: String,
      default: ''
    },
    // 图标类名
    iconClass: {
      type: String,
      default: ''
    },
    // 是否显示按钮
    showButton: {
      type: Boolean,
      default: false
    },
    // 按钮文本
    buttonText: {
      type: String,
      default: '按钮'
    },
    // 按钮是否禁用
    buttonDisabled: {
      type: Boolean,
      default: false
    }
  },

  methods: {
    // 处理输入事件
    handleInput(event) {
      this.$emit('input', event.target.value);
    },
    
    // 处理按钮点击事件
    handleButtonClick() {
      this.$emit('button-click');
    }
  }
}
</script>

<style scoped>
.custom-input {
  margin-bottom: 20px;
}

.input-wrapper {
  position: relative;
  background: white;
  border-radius: 24px;
  padding: 0 20px;
  
  border: 1px solid #eee;
  display: flex;
  align-items: center;
  min-height: 48px;
}

.icon {
  font-size: 16px;
  margin-right: 12px;
  color: #666;
}

.input-field {
  flex: 1;
  border: none;
  outline: none;
  /* font-size: 16px; */
  color: #333;
  background: transparent;
}

.input-field::placeholder {
  color: #ccc;
}

.action-button {
  background: #f0f0f0;
  border: none;
  border-radius: 16px;
  padding: 8px 16px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.action-button:not(:disabled):hover {
  background: #e0e0e0;
}

.action-button:disabled {
  background: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}
</style>
